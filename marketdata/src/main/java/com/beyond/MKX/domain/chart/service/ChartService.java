package com.beyond.MKX.domain.chart.service;

import com.beyond.MKX.domain.chart.dto.HourlyCloseData;
import com.beyond.MKX.domain.chart.dto.MiniChartResDTO;
import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.chart.repository.CandleInfluxRepository;
import com.beyond.MKX.domain.execution.dto.ExecutionEventDTO;
import com.beyond.MKX.domain.execution.entity.Execution;
import com.beyond.MKX.domain.execution.repository.ExecutionInfluxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ✅ 개선된 차트 데이터 관리 서비스
 * 
 * 핵심 개선사항:
 * 1. InfluxDB 체결 데이터를 Source of Truth로 사용
 * 2. Redis는 캐싱 용도로만 사용
 * 3. 캔들 재계산 기능 제공
 * 4. 데이터 정합성 보장
 * 
 * 처리 방식:
 * - 실시간: 체결 발생 시 Redis 증분 업데이트 (성능)
 * - 확정: 스케줄러가 InfluxDB 체결 데이터로 재계산 (정확성)
 * - 조회: InfluxDB 우선, Redis 캐시 활용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChartService {

    private final ExecutionInfluxRepository executionInfluxRepository;
    private final CandleInfluxRepository candleInfluxRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CURRENT_CANDLE_PREFIX = "candle:";
    
    private static final Map<String, Long> INTERVAL_MINUTES = Map.of(
            "1m", 1L,
            "5m", 5L,
            "15m", 15L,
            "30m", 30L,
            "1h", 60L,
            "4h", 240L,
            "1d", 1440L
    );

    /**
     * ✅ 실시간 체결 데이터로 캔들 증분 업데이트
     * 
     * Redis에 임시 저장 (빠른 응답)
     * 정확성은 스케줄러의 재계산으로 보장
     */
    public void updateChartData(ExecutionEventDTO execution) {
        try {
            log.debug("[CHART/UPDATE] Updating chart data for execution: {}", execution);
            
            for (String interval : INTERVAL_MINUTES.keySet()) {
                Candle candle = updateCurrentCandleIncremental(execution, interval);
                publishCandle(candle);
            }
            
        } catch (Exception e) {
            log.error("[CHART/UPDATE] Failed to update chart data", e);
        }
    }

    /**
     * ✅ 핵심: InfluxDB 체결 데이터 기반 캔들 재계산
     * 
     * Source of Truth = 체결 데이터
     * 스케줄러가 주기적으로 호출하여 정확한 캔들 생성
     * 
     * @param ticker 종목 코드
     * @param interval 캔들 간격
     * @param candleTime 캔들 시작 시각
     * @return 재계산된 정확한 캔들
     */
    public Candle recalculateCandleFromExecutions(String ticker, String interval, Instant candleTime) {
        try {
            long intervalMinutes = INTERVAL_MINUTES.get(interval);
            Instant start = candleTime;
            Instant end = candleTime.plus(intervalMinutes, ChronoUnit.MINUTES);
            
            log.info("[CHART/RECALC] Recalculating candle: ticker={}, interval={}, period={}~{}", 
                    ticker, interval, start, end);
            
            // ✅ InfluxDB에서 해당 기간의 체결 데이터 조회
            List<Execution> executions = executionInfluxRepository.findExecutions(ticker, start, end);
            
            if (executions.isEmpty()) {
                log.warn("[CHART/RECALC] No executions found, creating empty candle");
                return createEmptyCandleFromPrevious(ticker, interval, candleTime);
            }
            
            // ✅ 체결 데이터로부터 정확한 OHLCV 계산
            Candle candle = calculateOHLCVFromExecutions(ticker, interval, candleTime, executions);
            
            // InfluxDB에 확정된 캔들 저장
            candleInfluxRepository.save(candle);
            
            // Redis에 캐싱 (조회 성능)
            cacheCandle(candle, intervalMinutes);
            
            log.info("[CHART/RECALC] ✅ Recalculated: ticker={}, interval={}, O={}, H={}, L={}, C={}, V={}", 
                    ticker, interval, candle.getOpen(), candle.getHigh(), 
                    candle.getLow(), candle.getClose(), candle.getVolume());
            
            return candle;
            
        } catch (Exception e) {
            log.error("[CHART/RECALC] Failed to recalculate candle", e);
            throw new RuntimeException("Failed to recalculate candle", e);
        }
    }

    /**
     * ✅ 핵심 계산 로직: 체결 데이터 리스트로부터 OHLCV 계산
     * 
     * 이 메서드가 캔들 계산의 Source of Truth
     */
    private Candle calculateOHLCVFromExecutions(
            String ticker, String interval, Instant candleTime, List<Execution> executions) {
        
        if (executions.isEmpty()) {
            throw new IllegalArgumentException("Cannot calculate OHLCV from empty executions");
        }
        
        // ✅ 시간순 정렬 (중요!)
        executions = executions.stream()
                .sorted(Comparator.comparing(Execution::getTimestamp))
                .collect(Collectors.toList());
        
        // ✅ 정확한 OHLCV 계산
        long open = executions.get(0).getPrice();  // 첫 체결가
        long close = executions.get(executions.size() - 1).getPrice();  // 마지막 체결가
        
        long high = executions.stream()
                .mapToLong(Execution::getPrice)
                .max()
                .orElse(open);
        
        long low = executions.stream()
                .mapToLong(Execution::getPrice)
                .min()
                .orElse(open);
        
        BigDecimal volume = executions.stream()
                .map(Execution::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.debug("[CHART/CALC] Calculated from {} executions: O={}, H={}, L={}, C={}, V={}", 
                executions.size(), open, high, low, close, volume);
        
        return Candle.builder()
                .ticker(ticker)
                .interval(interval)
                .time(candleTime)
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .volume(volume)
                .build();
    }

    /**
     * 실시간 증분 업데이트 (성능 최적화)
     * 
     * Redis에서 현재 캔들을 가져와 업데이트
     * 정확성은 스케줄러의 재계산으로 보장
     */
    private Candle updateCurrentCandleIncremental(ExecutionEventDTO execution, String interval) {
        String redisKey = buildCurrentCandleKey(execution.getTicker(), interval);
        Candle candle = getCandleFromRedis(redisKey);
        Instant candleTime = getCandleTime(execution.getTimestamp(), interval);
        
        // 새 캔들 시작 또는 캔들이 없으면 생성
        if (candle == null || candle.getTime() == null || !candle.getTime().equals(candleTime)) {
            candle = Candle.builder()
                    .ticker(execution.getTicker())
                    .interval(interval)
                    .time(candleTime)
                    .open(execution.getPrice())
                    .high(execution.getPrice())
                    .low(execution.getPrice())
                    .close(execution.getPrice())
                    .volume(BigDecimal.ZERO)
                    .build();
        }
        
        // 증분 업데이트
        candle.update(execution.getPrice(), execution.getQuantity());
        
        // Redis 임시 저장 (짧은 TTL - 스케줄러가 재계산할 것)
        long ttlMinutes = INTERVAL_MINUTES.get(interval) * 2;
        redisTemplate.opsForValue().set(redisKey, candle, ttlMinutes, TimeUnit.MINUTES);
        
        log.debug("[CHART/UPDATE] Updated candle: ticker={}, interval={}, time={}, close={}", 
                execution.getTicker(), interval, candleTime, candle.getClose());
        
        return candle;
    }

    /**
     * 빈 캔들 생성 (거래 없는 구간)
     * 이전 캔들의 종가를 그대로 사용
     */
    private Candle createEmptyCandleFromPrevious(String ticker, String interval, Instant candleTime) {
        Candle previousCandle = candleInfluxRepository.findLatestCandle(ticker, interval);
        
        if (previousCandle == null) {
            log.warn("[CHART/RECALC] No previous candle found for ticker={}, interval={}", ticker, interval);
            return null;
        }
        
        return Candle.builder()
                .ticker(ticker)
                .interval(interval)
                .time(candleTime)
                .open(previousCandle.getClose())
                .high(previousCandle.getClose())
                .low(previousCandle.getClose())
                .close(previousCandle.getClose())
                .volume(BigDecimal.ZERO)
                .build();
    }

    /**
     * 캔들 캐싱 (조회 성능 향상)
     */
    private void cacheCandle(Candle candle, long intervalMinutes) {
        String redisKey = buildCurrentCandleKey(candle.getTicker(), candle.getInterval());
        long ttlMinutes = intervalMinutes * 100;
        redisTemplate.opsForValue().set(redisKey, candle, ttlMinutes, TimeUnit.MINUTES);
        log.debug("[CHART/CACHE] Cached candle: ticker={}, interval={}, time={}", 
                candle.getTicker(), candle.getInterval(), candle.getTime());
    }

    /**
     * ✅ 개선된 캔들 조회
     * 
     * 1. InfluxDB 캔들 버킷에서 확정된 캔들 조회
     * 2. Redis에서 현재 진행중인 캔들 추가
     * 3. 없으면 체결 데이터로 재계산 가능
     */
    public List<Candle> getCandles(String ticker, String interval, Instant start, Instant end) {
        try {
            log.info("[CHART/QUERY] Querying candles: ticker={}, interval={}, start={}, end={}", 
                    ticker, interval, start, end);
            
            List<Candle> allCandles = new ArrayList<>();
            
            // 1. InfluxDB에서 확정된 캔들 조회
            List<Candle> influxCandles = candleInfluxRepository.findCandles(ticker, interval, start, end);
            allCandles.addAll(influxCandles);
            log.info("[CHART/QUERY] Retrieved {} candles from InfluxDB", influxCandles.size());
            
            // 2. Redis에서 현재 진행중인 캔들 추가
            String currentCandleKey = buildCurrentCandleKey(ticker, interval);
            Candle currentCandle = getCandleFromRedis(currentCandleKey);
            
            if (currentCandle != null && currentCandle.getTime() != null) {
                if (!currentCandle.getTime().isBefore(start) && currentCandle.getTime().isBefore(end)) {
                    boolean exists = allCandles.stream()
                            .anyMatch(c -> c.getTime().equals(currentCandle.getTime()));
                    
                    if (!exists) {
                        allCandles.add(currentCandle);
                        log.info("[CHART/QUERY] Added current candle from Redis: time={}", currentCandle.getTime());
                    }
                }
            }
            
            // 3. 시간순 정렬
            allCandles = allCandles.stream()
                    .sorted(Comparator.comparing(Candle::getTime))
                    .collect(Collectors.toList());
            
            log.info("[CHART/QUERY] Total {} candles retrieved", allCandles.size());
            return allCandles;
            
        } catch (Exception e) {
            log.error("[CHART/QUERY] Failed to get candles", e);
            return Collections.emptyList();
        }
    }

    /**
     * 최근 N개의 캔들 조회 (STOMP 초기 구독 시 사용)
     */
    public List<Candle> getRecentCandles(String ticker, String interval, int limit) {
        try {
            Instant end = Instant.now();
            long intervalMinutes = INTERVAL_MINUTES.getOrDefault(interval, 1L);
            long lookbackMinutes = intervalMinutes * limit * 2;
            Instant start = end.minus(lookbackMinutes, ChronoUnit.MINUTES);
            
            List<Candle> candles = getCandles(ticker, interval, start, end);
            
            return candles.stream()
                    .sorted(Comparator.comparing(Candle::getTime).reversed())
                    .limit(limit)
                    .sorted(Comparator.comparing(Candle::getTime))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("[CHART/QUERY] Failed to get recent candles: ticker={}, interval={}, limit={}", 
                    ticker, interval, limit, e);
            return Collections.emptyList();
        }
    }

    /**
     * 최신 캔들 조회
     */
    public Candle getLatestCandle(String ticker, String interval) {
        String redisKey = buildCurrentCandleKey(ticker, interval);
        Candle candle = getCandleFromRedis(redisKey);
        
        if (candle == null) {
            log.warn("[CHART/QUERY] No latest candle found for ticker={}, interval={}", ticker, interval);
            return null;
        }
        
        return candle;
    }

    // ========== 유틸리티 메서드 ==========

    private Instant getCandleTime(long timestamp, String interval) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        long intervalMinutes = INTERVAL_MINUTES.get(interval);
        
        if ("1d".equals(interval)) {
            return instant.atZone(ZoneId.of("UTC"))
                    .truncatedTo(ChronoUnit.DAYS)
                    .toInstant();
        }
        
        long epochMinutes = instant.getEpochSecond() / 60;
        long candleMinutes = (epochMinutes / intervalMinutes) * intervalMinutes;
        return Instant.ofEpochSecond(candleMinutes * 60);
    }

    private String buildCurrentCandleKey(String ticker, String interval) {
        return CURRENT_CANDLE_PREFIX + ticker + ":" + interval;
    }

    private Candle getCandleFromRedis(String redisKey) {
        try {
            Object data = redisTemplate.opsForValue().get(redisKey);
            if (data == null) {
                return null;
            }
            
            if (data instanceof Candle) {
                return (Candle) data;
            }
            
            return objectMapper.convertValue(data, Candle.class);
        } catch (Exception e) {
            log.warn("[CHART/QUERY] Failed to deserialize Candle from Redis", e);
            return null;
        }
    }

    private void publishCandle(Candle candle) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "chart");
            message.put("ticker", candle.getTicker());
            message.put("data", Map.of(
                    "ticker", candle.getTicker(),
                    "interval", candle.getInterval(),
                    "time", candle.getTime(),
                    "open", candle.getOpen(),
                    "high", candle.getHigh(),
                    "low", candle.getLow(),
                    "close", candle.getClose(),
                    "volume", candle.getVolume()
            ));
            message.put("timestamp", System.currentTimeMillis());
            
            redisTemplate.convertAndSend("market:chart", message);
            
            log.debug("[CHART-STOMP] 📤 Published: channel=market:chart, ticker={}, interval={}, close={}",
                    candle.getTicker(), candle.getInterval(), candle.getClose());
            
        } catch (Exception e) {
            log.error("[CHART-STOMP] ❌ Failed to publish: ticker={}", candle.getTicker(), e);
        }
    }

    // ========== 미니차트 API ==========

    /**
     * 여러 종목의 24시간 종가 데이터 조회 (미니차트용)
     * 
     * 1시간봉 데이터에서 종가만 추출하여 반환
     * 한번의 DB 조회로 모든 종목 데이터를 가져와 성능 최적화
     * 
     * @param tickers 종목 코드 리스트
     * @return 종목별 24시간 종가 데이터 리스트
     */
    public List<MiniChartResDTO> get24HourClosesForTickers(List<String> tickers) {
        try {
            Instant now = Instant.now();
            Instant start = now.minus(24, ChronoUnit.HOURS);

            log.info("[CHART/MINI] Querying 24h closes for {} tickers", tickers.size());

            // 한번의 DB 조회로 모든 종목의 1시간봉 데이터 가져오기
            Map<String, List<Candle>> candlesMap = 
                candleInfluxRepository.findCandlesForTickers(tickers, "1h", start, now);

            // 각 ticker별로 DTO 생성
            List<MiniChartResDTO> result = tickers.stream()
                    .map(ticker -> {
                        List<Candle> candles = candlesMap.getOrDefault(ticker, Collections.emptyList());

                        // 종가만 추출
                        List<HourlyCloseData> hourlyCloses = candles.stream()
                                .map(candle -> HourlyCloseData.builder()
                                        .timestamp(candle.getTime())
                                        .close(candle.getClose())
                                        .build())
                                .collect(Collectors.toList());

                        return MiniChartResDTO.builder()
                                .ticker(ticker)
                                .hourlyCloses(hourlyCloses)
                                .build();
                    })
                    .collect(Collectors.toList());

            log.info("[CHART/MINI] Retrieved 24h closes for {} tickers (total {} data points)", 
                    tickers.size(), result.stream().mapToInt(r -> r.getHourlyCloses().size()).sum());

            return result;

        } catch (Exception e) {
            log.error("[CHART/MINI] Failed to get 24h closes for tickers: {}", tickers, e);
            return Collections.emptyList();
        }
    }
}
