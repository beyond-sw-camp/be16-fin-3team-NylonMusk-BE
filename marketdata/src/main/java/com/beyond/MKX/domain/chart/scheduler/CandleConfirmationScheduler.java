package com.beyond.MKX.domain.chart.scheduler;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.chart.repository.CandleInfluxRepository;
import com.beyond.MKX.domain.chart.websocket.ChartWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 캔들 확정 및 생성 스케줄러
 * 
 * 매 interval마다 실제 시간 기준으로 캔들 생성
 * - 체결이 있으면: Redis의 캔들을 InfluxDB로 저장
 * - 체결이 없으면: 이전 캔들과 동일한 OHLC로 빈 캔들 생성 (거래량 0)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CandleConfirmationScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CandleInfluxRepository candleInfluxRepository;
    private final ChartWebSocketHandler chartWebSocketHandler;  // ✅ WebSocket 추가
    private final ObjectMapper objectMapper;

    private static final String CURRENT_CANDLE_PREFIX = "candle:";
    
    // 추적할 종목 리스트
    private final Set<String> trackedTickers = new HashSet<>();

    /**
     * 1분마다 실행 - 1분 캔들 생성
     */
    @Scheduled(cron = "0 * * * * *")
    public void confirm1MinuteCandles() {
        processCandles("1m", 1);
    }

    /**
     * 5분마다 실행 - 5분 캔들 생성
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void confirm5MinuteCandles() {
        processCandles("5m", 5);
    }

    /**
     * 15분마다 실행 - 15분 캔들 생성
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void confirm15MinuteCandles() {
        processCandles("15m", 15);
    }

    /**
     * 30분마다 실행 - 30분 캔들 생성
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void confirm30MinuteCandles() {
        processCandles("30m", 30);
    }

    /**
     * 1시간마다 실행 - 1시간 캔들 생성
     */
    @Scheduled(cron = "0 0 * * * *")
    public void confirm1HourCandles() {
        processCandles("1h", 60);
    }

    /**
     * 4시간마다 실행 - 4시간 캔들 생성
     */
    @Scheduled(cron = "0 0 */4 * * *")
    public void confirm4HourCandles() {
        processCandles("4h", 240);
    }

    /**
     * 매일 자정 실행 - 1일 캔들 생성
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void confirm1DayCandles() {
        processCandles("1d", 1440);
    }

    /**
     * 캔들 처리 - 실제 시간 기준
     */
    private void processCandles(String interval, long intervalMinutes) {
        try {
            log.info("[CANDLE/SCHEDULER] ========== Starting: interval={} ==========", interval);
            
            // 현재 시각 기준으로 직전 interval의 시작 시각 계산
            Instant now = Instant.now();
            Instant candleTime = calculateCandleTime(now, intervalMinutes);
            
            log.info("[CANDLE/SCHEDULER] Candle time: {}, interval: {}", candleTime, interval);
            
            // 활성 종목 수집
            updateTrackedTickers();
            
            if (trackedTickers.isEmpty()) {
                log.warn("[CANDLE/SCHEDULER] No tickers found");
                return;
            }
            
            log.info("[CANDLE/SCHEDULER] Processing {} tickers", trackedTickers.size());
            
            List<Candle> candlesToSave = new ArrayList<>();
            int confirmedCount = 0;
            int generatedCount = 0;
            int skippedCount = 0;
            
            // 각 종목별 캔들 생성
            for (String ticker : trackedTickers) {
                try {
                    Candle candle = createOrGetCandle(ticker, interval, candleTime);
                    
                    if (candle != null) {
                        candlesToSave.add(candle);
                        
                        if (candle.getVolume() != null && candle.getVolume().compareTo(BigDecimal.ZERO) > 0) {
                            confirmedCount++;
                            log.debug("[CANDLE/SCHEDULER] ✅ Confirmed: ticker={}, time={}, close={}, volume={}",
                                    ticker, candle.getTime(), candle.getClose(), candle.getVolume());
                        } else {
                            generatedCount++;
                            log.debug("[CANDLE/SCHEDULER] 📊 Generated: ticker={}, time={}, close={} (no volume)",
                                    ticker, candle.getTime(), candle.getClose());
                        }
                    } else {
                        skippedCount++;
                        log.debug("[CANDLE/SCHEDULER] ⏭️ Skipped: ticker={} (no previous candle)", ticker);
                    }
                    
                } catch (Exception e) {
                    log.error("[CANDLE/SCHEDULER] Failed to process ticker: {}", ticker, e);
                }
            }
            
            // InfluxDB에 배치 저장
            if (!candlesToSave.isEmpty()) {
                candleInfluxRepository.saveAll(candlesToSave);
                log.info("[CANDLE/SCHEDULER] ✅ Saved {} candles (confirmed={}, generated={}, skipped={})", 
                        candlesToSave.size(), confirmedCount, generatedCount, skippedCount);
                
                // ✅ 모든 캔들을 WebSocket으로 브로드캠스트
                for (Candle candle : candlesToSave) {
                    try {
                        chartWebSocketHandler.broadcastCandle(candle.getTicker(), candle);
                        log.debug("[CANDLE/SCHEDULER] 📡 Broadcasted: ticker={}, interval={}, time={}, volume={}",
                                candle.getTicker(), candle.getInterval(), candle.getTime(), candle.getVolume());
                    } catch (Exception e) {
                        log.error("[CANDLE/SCHEDULER] Failed to broadcast candle: ticker={}", candle.getTicker(), e);
                    }
                }
            } else {
                log.warn("[CANDLE/SCHEDULER] ⚠️ No candles to save (skipped={})", skippedCount);
            }
            
            log.info("[CANDLE/SCHEDULER] ========== Completed: interval={} ==========", interval);
            
        } catch (Exception e) {
            log.error("[CANDLE/SCHEDULER] ❌ Failed: interval={}", interval, e);
        }
    }

    /**
     * 캔들 생성 또는 조회
     * 1. Redis에서 현재 진행중인 캔들 확인
     * 2. 없으면 이전 캔들 기준으로 빈 캔들 생성
     */
    private Candle createOrGetCandle(String ticker, String interval, Instant candleTime) {
        // 1. Redis에서 현재 캔들 확인
        String redisKey = CURRENT_CANDLE_PREFIX + ticker + ":" + interval;
        Object data = redisTemplate.opsForValue().get(redisKey);
        
        if (data != null) {
            // Redis에 캔들이 있으면 변환
            Candle candle = convertToCandle(data, ticker, interval);
            if (candle != null && candle.getTime() != null) {
                // 해당 시간대 캔들이면 반환
                if (candle.getTime().equals(candleTime)) {
                    return candle;
                }
            }
        }
        
        // 2. Redis에 없거나 다른 시간대면 이전 캔들 기준으로 빈 캔들 생성
        return generateCandleFromPrevious(ticker, interval, candleTime);
    }

    /**
     * 이전 캔들 기준으로 빈 캔들 생성
     */
    private Candle generateCandleFromPrevious(String ticker, String interval, Instant time) {
        try {
            // InfluxDB에서 이전 캔들 조회
            Candle previousCandle = candleInfluxRepository.findLatestCandle(ticker, interval);
            
            if (previousCandle != null) {
                // 이전 캔들의 종가를 그대로 사용 (변동 없음)
                return Candle.builder()
                        .ticker(ticker)
                        .interval(interval)
                        .time(time)
                        .open(previousCandle.getClose())
                        .high(previousCandle.getClose())
                        .low(previousCandle.getClose())
                        .close(previousCandle.getClose())
                        .volume(BigDecimal.ZERO)
                        .build();
            } else {
                // 이전 캔들이 없으면 null 반환 (최초 캔들은 체결이 있어야 생성)
                log.debug("[CANDLE/SCHEDULER] No previous candle for ticker: {}", ticker);
                return null;
            }
            
        } catch (Exception e) {
            log.error("[CANDLE/SCHEDULER] Failed to generate empty candle: ticker={}", ticker, e);
            return null;
        }
    }

    /**
     * 캔들 시작 시각 계산 (실제 시간 기준)
     * 예: 19:05:00에 실행 → 19:04:00~19:05:00 구간의 캔들 생성 → 19:04:00
     */
    private Instant calculateCandleTime(Instant now, long intervalMinutes) {
        if (intervalMinutes == 1440) {
            // 1일 캔들: 어제 자정
            return now.atZone(ZoneId.of("UTC"))
                    .minusDays(1)
                    .truncatedTo(ChronoUnit.DAYS)
                    .toInstant();
        }
        
        // 분 단위 캔들: 직전 interval의 시작 시각
        long epochMinutes = now.getEpochSecond() / 60;
        // 현재 interval의 시작 시각에서 interval만큼 뺀면 직전 interval
        long currentIntervalStart = (epochMinutes / intervalMinutes) * intervalMinutes;
        long previousIntervalStart = currentIntervalStart - intervalMinutes;
        
        return Instant.ofEpochSecond(previousIntervalStart * 60);
    }

    /**
     * Redis에서 활성 종목 수집
     */
    private void updateTrackedTickers() {
        try {
            Set<String> tickers = new HashSet<>();
            
            // 1. 캔들 키에서 수집
            Set<String> candleKeys = redisTemplate.keys("candle:*:*");
            if (candleKeys != null) {
                for (String key : candleKeys) {
                    String[] parts = key.split(":");
                    if (parts.length >= 2) {
                        tickers.add(parts[1]);
                    }
                }
            }
            
            // 2. 호가 키에서 수집
            Set<String> orderBookKeys = redisTemplate.keys("orderbook:*");
            if (orderBookKeys != null) {
                for (String key : orderBookKeys) {
                    String ticker = key.substring("orderbook:".length());
                    tickers.add(ticker);
                }
            }
            
            // 3. 현재가 키에서 수집
            Set<String> priceKeys = redisTemplate.keys("price:*");
            if (priceKeys != null) {
                for (String key : priceKeys) {
                    String ticker = key.substring("price:".length());
                    tickers.add(ticker);
                }
            }
            
            trackedTickers.clear();
            trackedTickers.addAll(tickers);
            
            log.debug("[CANDLE/SCHEDULER] Tracked tickers: {}", trackedTickers);
            
        } catch (Exception e) {
            log.error("[CANDLE/SCHEDULER] Failed to update tickers", e);
        }
    }

    /**
     * Object를 Candle로 변환
     */
    private Candle convertToCandle(Object data, String ticker, String interval) {
        try {
            if (data instanceof Candle) {
                return (Candle) data;
            }
            
            if (data instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) data;
                
                Candle candle = new Candle();
                candle.setTicker(ticker);
                candle.setInterval(interval);
                
                // OHLC
                if (map.get("open") != null) {
                    candle.setOpen(((Number) map.get("open")).longValue());
                }
                if (map.get("high") != null) {
                    candle.setHigh(((Number) map.get("high")).longValue());
                }
                if (map.get("low") != null) {
                    candle.setLow(((Number) map.get("low")).longValue());
                }
                if (map.get("close") != null) {
                    candle.setClose(((Number) map.get("close")).longValue());
                }
                
                // Time
                Object timeObj = map.get("time");
                if (timeObj != null) {
                    if (timeObj instanceof String) {
                        candle.setTime(Instant.parse((String) timeObj));
                    } else if (timeObj instanceof Long) {
                        candle.setTime(Instant.ofEpochMilli((Long) timeObj));
                    } else if (timeObj instanceof Map) {
                        Map<?, ?> timeMap = (Map<?, ?>) timeObj;
                        long epochSecond = ((Number) timeMap.get("epochSecond")).longValue();
                        int nano = timeMap.containsKey("nano") ? ((Number) timeMap.get("nano")).intValue() : 0;
                        candle.setTime(Instant.ofEpochSecond(epochSecond, nano));
                    }
                }
                
                // Volume
                Object volumeObj = map.get("volume");
                if (volumeObj != null) {
                    if (volumeObj instanceof BigDecimal) {
                        candle.setVolume((BigDecimal) volumeObj);
                    } else if (volumeObj instanceof Number) {
                        candle.setVolume(new BigDecimal(volumeObj.toString()));
                    }
                } else {
                    candle.setVolume(BigDecimal.ZERO);
                }
                
                return candle;
            }
            
            return objectMapper.convertValue(data, Candle.class);
            
        } catch (Exception e) {
            log.error("[CANDLE/SCHEDULER] Failed to convert to candle", e);
            return null;
        }
    }
}
