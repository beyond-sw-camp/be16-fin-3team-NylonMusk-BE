package com.beyond.MKX.domain.chart.service;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.chart.repository.CandleInfluxRepository;
import com.beyond.MKX.domain.chart.websocket.ChartWebSocketHandler;
import com.beyond.MKX.domain.execution.dto.ExecutionEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 차트 데이터 관리 서비스 (하이브리드 방식)
 * 
 * OHLCV 캔들스틱 차트 데이터를 관리하고 실시간으로 업데이트
 * 
 * 캔들 저장 전략:
 * 1. 현재 진행 중인 캔들: Redis (실시간 업데이트)
 * 2. 확정된 캔들: Redis (7일 TTL) + InfluxDB (영구 보관)
 * 
 * 조회 전략:
 * 1. 최근 데이터: Redis에서 조회 (빠름)
 * 2. 과거 데이터: InfluxDB에서 조회 (느림)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CandleInfluxRepository candleInfluxRepository;
    private final ChartWebSocketHandler chartWebSocketHandler;
    private final ObjectMapper objectMapper;

    // Redis key prefix
    private static final String CURRENT_CANDLE_PREFIX = "candle:";  // 현재 진행 중인 캔들
    private static final String CONFIRMED_CANDLE_PREFIX = "candle:confirmed:";  // 확정된 캔들
    
    // 지원하는 캔들 간격
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
     * 체결 데이터로 차트 업데이트
     * 모든 interval에 대해 현재 진행 중인 캔들을 업데이트하고 WebSocket으로 전송
     */
    public void updateChartData(ExecutionEventDTO execution) {
        try {
            log.debug("[CHART/UPDATE] Updating chart data for execution: {}", execution);
            
            // 모든 interval에 대해 캔들 업데이트
            for (String interval : INTERVAL_MINUTES.keySet()) {
                Candle candle = updateCurrentCandle(execution, interval);
                
                // WebSocket으로 실시간 전송
                chartWebSocketHandler.broadcastCandle(execution.getTicker(), candle);
            }
            
        } catch (Exception e) {
            log.error("[CHART/UPDATE] Failed to update chart data", e);
        }
    }

    /**
     * 특정 interval의 현재 진행 중인 캔들 업데이트
     */
    private Candle updateCurrentCandle(ExecutionEventDTO execution, String interval) {
        String redisKey = buildCurrentCandleKey(execution.getTicker(), interval);
        
        // Redis에서 현재 캔들 조회
        Candle candle = getCandleFromRedis(redisKey);
        
        Instant candleTime = getCandleTime(execution.getTimestamp(), interval);
        
        // 캔들이 없거나 새로운 캔들 시작 시간인 경우
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
        
        // 캔들 데이터 업데이트
        candle.update(execution.getPrice(), execution.getQuantity());
        
        // Redis에 저장 (TTL: interval에 따라 다르게 설정)
        long ttlMinutes = INTERVAL_MINUTES.get(interval) * 100;
        redisTemplate.opsForValue().set(redisKey, candle, ttlMinutes, TimeUnit.MINUTES);
        
        // ✅ InfluxDB에도 즉시 저장 (이전 캔들 조회를 위해)
        try {
            candleInfluxRepository.save(candle);
            log.debug("[CHART/UPDATE] Saved to InfluxDB: ticker={}, interval={}, time={}", 
                    execution.getTicker(), interval, candleTime);
        } catch (Exception e) {
            log.error("[CHART/UPDATE] Failed to save to InfluxDB", e);
        }
        
        log.debug("[CHART/UPDATE] Updated current candle: ticker={}, interval={}, time={}", 
                execution.getTicker(), interval, candleTime);
        
        return candle;
    }

    /**
     * 특정 종목의 캔들 데이터 조회 (하이브리드 방식)
     * 1. Redis에서 확정된 캔들 조회 (7일 이내)
     * 2. 부족한 부분은 InfluxDB에서 조회
     */
    public List<Candle> getCandles(String ticker, String interval, Instant start, Instant end) {
        try {
            log.info("[CHART/QUERY] Querying candles: ticker={}, interval={}, start={}, end={}", 
                    ticker, interval, start, end);
            
            List<Candle> allCandles = new ArrayList<>();
            
            // 1. Redis에서 확정된 캔들 조회 (최근 7일)
            Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
            Instant redisStart = start.isAfter(sevenDaysAgo) ? start : sevenDaysAgo;
            
            if (redisStart.isBefore(end)) {
                List<Candle> redisCandles = getCandlesFromRedis(ticker, interval, redisStart, end);
                allCandles.addAll(redisCandles);
                log.info("[CHART/QUERY] Retrieved {} candles from Redis", redisCandles.size());
            }
            
            // 2. Redis에서 커버하지 못한 과거 데이터는 InfluxDB에서 조회
            if (start.isBefore(sevenDaysAgo)) {
                Instant influxEnd = end.isBefore(sevenDaysAgo) ? end : sevenDaysAgo;
                List<Candle> influxCandles = candleInfluxRepository.findCandles(ticker, interval, start, influxEnd);
                allCandles.addAll(influxCandles);
                log.info("[CHART/QUERY] Retrieved {} candles from InfluxDB", influxCandles.size());
            }
            
            // 3. 시간순 정렬 및 중복 제거
            allCandles = allCandles.stream()
                    .sorted(Comparator.comparing(Candle::getTime))
                    .distinct()
                    .collect(Collectors.toList());
            
            log.info("[CHART/QUERY] Total {} candles retrieved", allCandles.size());
            return allCandles;
            
        } catch (Exception e) {
            log.error("[CHART/QUERY] Failed to get candles", e);
            return Collections.emptyList();
        }
    }

    /**
     * Redis에서 확정된 캔들 조회
     */
    private List<Candle> getCandlesFromRedis(String ticker, String interval, Instant start, Instant end) {
        try {
            // Redis에서 해당 ticker와 interval의 확정된 캔들 패턴 조회
            String pattern = CONFIRMED_CANDLE_PREFIX + ticker + ":" + interval + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys == null || keys.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<Candle> candles = new ArrayList<>();
            
            for (String key : keys) {
                Object data = redisTemplate.opsForValue().get(key);
                if (data == null) {
                    continue;
                }
                
                Candle candle = convertToCandle(data);
                if (candle != null && candle.getTime() != null) {
                    // 시간 범위 필터링
                    if (!candle.getTime().isBefore(start) && candle.getTime().isBefore(end)) {
                        candles.add(candle);
                    }
                }
            }
            
            return candles;
            
        } catch (Exception e) {
            log.error("[CHART/QUERY] Failed to get candles from Redis", e);
            return Collections.emptyList();
        }
    }

    /**
     * 최신 캔들 조회 (Redis에서 현재 진행 중인 캔들)
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

    /**
     * Redis에서 Candle 객체를 안전하게 조회
     */
    private Candle getCandleFromRedis(String redisKey) {
        try {
            Object data = redisTemplate.opsForValue().get(redisKey);
            
            if (data == null) {
                return null;
            }
            
            return convertToCandle(data);
            
        } catch (Exception e) {
            log.warn("[CHART/QUERY] Failed to deserialize Candle from Redis: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 데이터를 Candle 객체로 변환
     */
    private Candle convertToCandle(Object data) {
        try {
            // 이미 Candle 타입인 경우
            if (data instanceof Candle) {
                return (Candle) data;
            }
            
            // LinkedHashMap 등 다른 타입인 경우 ObjectMapper로 변환
            return objectMapper.convertValue(data, Candle.class);
            
        } catch (Exception e) {
            log.warn("[CHART/QUERY] Failed to convert data to Candle: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 현재 진행 중인 캔들의 Redis key 생성
     */
    private String buildCurrentCandleKey(String ticker, String interval) {
        return CURRENT_CANDLE_PREFIX + ticker + ":" + interval;
    }

    /**
     * 체결 시각을 캔들 시작 시각으로 변환 (UTC 기준)
     */
    private Instant getCandleTime(long timestamp, String interval) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        long intervalMinutes = INTERVAL_MINUTES.get(interval);
        
        // 1일 캔들의 경우 UTC 날짜 기준으로 처리
        if ("1d".equals(interval)) {
            return instant.atZone(ZoneId.of("UTC"))  // ✅ UTC 고정
                    .truncatedTo(ChronoUnit.DAYS)
                    .toInstant();
        }
        
        // 분 단위 캔들의 경우
        long epochMinutes = instant.getEpochSecond() / 60;
        long candleMinutes = (epochMinutes / intervalMinutes) * intervalMinutes;
        
        return Instant.ofEpochSecond(candleMinutes * 60);
    }
}
