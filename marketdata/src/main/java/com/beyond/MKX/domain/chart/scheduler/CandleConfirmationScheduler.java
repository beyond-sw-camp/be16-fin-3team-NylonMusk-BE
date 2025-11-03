package com.beyond.MKX.domain.chart.scheduler;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.chart.service.ChartService;
import com.beyond.MKX.domain.chart.stomp.ChartStompController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * ✅ 개선된 캔들 확정 스케줄러
 * 
 * 핵심 개선사항:
 * 1. Redis가 아닌 InfluxDB 체결 데이터로 캔들 재계산
 * 2. 데이터 정합성 보장 (Source of Truth = 체결 데이터)
 * 3. Redis 유실 시에도 복구 가능
 * 
 * 동작 방식:
 * - 각 interval 종료 시 해당 기간의 체결 데이터 조회
 * - 체결 데이터로부터 OHLCV 재계산
 * - InfluxDB에 확정된 캔들 저장
 * - Redis에 캐싱
 * - WebSocket으로 브로드캐스트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CandleConfirmationScheduler {

    private final ChartService chartService;
    private final ChartStompController chartStompController;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final Map<String, Long> INTERVAL_MINUTES = Map.of(
            "1m", 1L,
            "5m", 5L,
            "15m", 15L,
            "30m", 30L,
            "1h", 60L,
            "4h", 240L,
            "1d", 1440L
    );

    private final Set<String> trackedTickers = new HashSet<>();

    /**
     * 1분마다 실행 - 1분 캔들 확정
     */
    @Scheduled(cron = "0 * * * * *")
    public void confirm1MinuteCandles() {
        confirmCandles("1m", 1);
    }

    /**
     * 5분마다 실행 - 5분 캔들 확정
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void confirm5MinuteCandles() {
        confirmCandles("5m", 5);
    }

    /**
     * 15분마다 실행 - 15분 캔들 확정
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void confirm15MinuteCandles() {
        confirmCandles("15m", 15);
    }

    /**
     * 30분마다 실행 - 30분 캔들 확정
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void confirm30MinuteCandles() {
        confirmCandles("30m", 30);
    }

    /**
     * 1시간마다 실행 - 1시간 캔들 확정
     */
    @Scheduled(cron = "0 0 * * * *")
    public void confirm1HourCandles() {
        confirmCandles("1h", 60);
    }

    /**
     * 4시간마다 실행 - 4시간 캔들 확정
     */
    @Scheduled(cron = "0 0 */4 * * *")
    public void confirm4HourCandles() {
        confirmCandles("4h", 240);
    }

    /**
     * 매일 자정 실행 - 1일 캔들 확정
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void confirm1DayCandles() {
        confirmCandles("1d", 1440);
    }

    /**
     * ✅ 개선된 캔들 확정 프로세스
     * 
     * InfluxDB 체결 데이터를 기반으로 재계산
     */
    private void confirmCandles(String interval, long intervalMinutes) {
        try {
            log.info("[CANDLE/CONFIRM] ========== Starting: interval={} ==========", interval);
            
            Instant now = Instant.now();
            Instant candleTime = calculatePreviousCandleTime(now, intervalMinutes);
            
            log.info("[CANDLE/CONFIRM] Confirming candle: time={}, interval={}", candleTime, interval);
            
            updateTrackedTickers();
            
            if (trackedTickers.isEmpty()) {
                log.warn("[CANDLE/CONFIRM] No tickers found");
                return;
            }
            
            log.info("[CANDLE/CONFIRM] Processing {} tickers", trackedTickers.size());
            
            int confirmedCount = 0;
            int emptyCount = 0;
            int failedCount = 0;
            
            for (String ticker : trackedTickers) {
                try {
                    // ✅ 핵심: InfluxDB 체결 데이터로 캔들 재계산
                    Candle candle = chartService.recalculateCandleFromExecutions(
                            ticker, interval, candleTime);
                    
                    if (candle != null) {
                        if (candle.getVolume() != null && candle.getVolume().compareTo(BigDecimal.ZERO) > 0) {
                            confirmedCount++;
                            log.info("[CANDLE/CONFIRM] ✅ Confirmed: ticker={}, time={}, O={}, H={}, L={}, C={}, V={}", 
                                    ticker, candleTime, candle.getOpen(), candle.getHigh(), 
                                    candle.getLow(), candle.getClose(), candle.getVolume());
                        } else {
                            emptyCount++;
                            log.debug("[CANDLE/CONFIRM] 📊 Empty candle: ticker={}, time={}, close={}", 
                                    ticker, candleTime, candle.getClose());
                        }
                        
                        chartStompController.publishCandle(candle);
                        
                    } else {
                        log.debug("[CANDLE/CONFIRM] ⏭️ Skipped: ticker={} (no previous candle)", ticker);
                    }
                    
                } catch (Exception e) {
                    failedCount++;
                    log.error("[CANDLE/CONFIRM] ❌ Failed: ticker={}", ticker, e);
                }
            }
            
            log.info("[CANDLE/CONFIRM] ========== Completed: interval={}, confirmed={}, empty={}, failed={} ==========", 
                    interval, confirmedCount, emptyCount, failedCount);
            
        } catch (Exception e) {
            log.error("[CANDLE/CONFIRM] ❌ Failed: interval={}", interval, e);
        }
    }

    /**
     * 직전 캔들 시작 시각 계산
     */
    private Instant calculatePreviousCandleTime(Instant now, long intervalMinutes) {
        if (intervalMinutes == 1440) {
            return now.atZone(ZoneId.of("UTC"))
                    .minusDays(1)
                    .truncatedTo(ChronoUnit.DAYS)
                    .toInstant();
        }
        
        long epochMinutes = now.getEpochSecond() / 60;
        long currentIntervalStart = (epochMinutes / intervalMinutes) * intervalMinutes;
        long previousIntervalStart = currentIntervalStart - intervalMinutes;
        
        return Instant.ofEpochSecond(previousIntervalStart * 60);
    }

    /**
     * 활성 종목 수집
     */
    private void updateTrackedTickers() {
        try {
            Set<String> tickers = new HashSet<>();
            
            Set<String> candleKeys = redisTemplate.keys("candle:*:*");
            if (candleKeys != null) {
                for (String key : candleKeys) {
                    String[] parts = key.split(":");
                    if (parts.length >= 2) {
                        tickers.add(parts[1]);
                    }
                }
            }
            
            Set<String> orderBookKeys = redisTemplate.keys("orderbook:*");
            if (orderBookKeys != null) {
                for (String key : orderBookKeys) {
                    String ticker = key.substring("orderbook:".length());
                    tickers.add(ticker);
                }
            }
            
            Set<String> priceKeys = redisTemplate.keys("price:*");
            if (priceKeys != null) {
                for (String key : priceKeys) {
                    String ticker = key.substring("price:".length());
                    tickers.add(ticker);
                }
            }
            
            trackedTickers.clear();
            trackedTickers.addAll(tickers);
            
            log.debug("[CANDLE/CONFIRM] Tracked tickers: {}", trackedTickers);
            
        } catch (Exception e) {
            log.error("[CANDLE/CONFIRM] Failed to update tickers", e);
        }
    }
}
