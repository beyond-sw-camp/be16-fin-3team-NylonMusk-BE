package com.beyond.MKX.domain.chart.scheduler;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.chart.service.ChartService;
import com.beyond.MKX.domain.chart.stomp.ChartStompController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
    @SchedulerLock(name = "confirm1MinuteCandles", lockAtMostFor = "50000", lockAtLeastFor = "10000")
    public void confirm1MinuteCandles() {
        confirmCandles("1m", 1);
    }

    /**
     * 5분마다 실행 - 5분 캔들 확정
     */
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "confirm5MinuteCandles", lockAtMostFor = "240000", lockAtLeastFor = "30000")
    public void confirm5MinuteCandles() {
        confirmCandles("5m", 5);
    }

    /**
     * 15분마다 실행 - 15분 캔들 확정
     */
    @Scheduled(cron = "0 */15 * * * *")
    @SchedulerLock(name = "confirm15MinuteCandles", lockAtMostFor = "840000", lockAtLeastFor = "60000")
    public void confirm15MinuteCandles() {
        confirmCandles("15m", 15);
    }

    /**
     * 30분마다 실행 - 30분 캔들 확정
     */
    @Scheduled(cron = "0 */30 * * * *")
    @SchedulerLock(name = "confirm30MinuteCandles", lockAtMostFor = "1740000", lockAtLeastFor = "60000")
    public void confirm30MinuteCandles() {
        confirmCandles("30m", 30);
    }

    /**
     * 1시간마다 실행 - 1시간 캔들 확정
     */
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "confirm1HourCandles", lockAtMostFor = "3540000", lockAtLeastFor = "120000")
    public void confirm1HourCandles() {
        confirmCandles("1h", 60);
    }

    /**
     * 4시간마다 실행 - 4시간 캔들 확정
     */
    @Scheduled(cron = "0 0 */4 * * *")
    @SchedulerLock(name = "confirm4HourCandles", lockAtMostFor = "13800000", lockAtLeastFor = "300000")
    public void confirm4HourCandles() {
        confirmCandles("4h", 240);
    }

    /**
     * 매일 자정 실행 - 1일 캔들 확정
     */
    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "confirm1DayCandles", lockAtMostFor = "82800000", lockAtLeastFor = "600000")
    public void confirm1DayCandles() {
        confirmCandles("1d", 1440);
    }

    /**
     * ✅ 개선된 캔들 확정 프로세스
     *
     * 1. 이전 캔들 확정 (InfluxDB 체결 데이터 기반)
     * 2. 현재 캔들 임시 생성 (거래 없어도 생성)
     */
    private void confirmCandles(String interval, long intervalMinutes) {
        try {
            log.info("[CANDLE/CONFIRM] ========== Starting: interval={} ==========", interval);

            Instant now = Instant.now();
            Instant previousCandleTime = calculatePreviousCandleTime(now, intervalMinutes);
            Instant currentCandleTime = calculateCurrentCandleTime(now, intervalMinutes);

            log.info("[CANDLE/CONFIRM] Previous candle: {}, Current candle: {}, interval={}",
                    previousCandleTime, currentCandleTime, interval);

            updateTrackedTickers();

            if (trackedTickers.isEmpty()) {
                log.warn("[CANDLE/CONFIRM] No tickers found");
                return;
            }

            log.info("[CANDLE/CONFIRM] Processing {} tickers", trackedTickers.size());

            int previousConfirmed = 0;
            int previousEmpty = 0;
            int currentConfirmed = 0;
            int currentEmpty = 0;
            int failedCount = 0;

            for (String ticker : trackedTickers) {
                try {
                    // 1️⃣ 이전 캔들 확정 (체결 데이터 기반)
                    Candle previousCandle = chartService.recalculateCandleFromExecutions(
                            ticker, interval, previousCandleTime);

                    if (previousCandle != null) {
                        if (previousCandle.getVolume() != null && previousCandle.getVolume().compareTo(BigDecimal.ZERO) > 0) {
                            previousConfirmed++;
                            log.info("[CANDLE/CONFIRM] ✅ Previous confirmed: ticker={}, time={}, O={}, H={}, L={}, C={}, V={}",
                                    ticker, previousCandleTime, previousCandle.getOpen(), previousCandle.getHigh(),
                                    previousCandle.getLow(), previousCandle.getClose(), previousCandle.getVolume());
                        } else {
                            previousEmpty++;
                            log.debug("[CANDLE/CONFIRM] 📊 Previous empty: ticker={}, time={}, close={}",
                                    ticker, previousCandleTime, previousCandle.getClose());
                        }
                        // ✅ ChartService에서 이미 publishCandle() 호출하므로 여기서 중복 발행 불필요
                    }

                    // 2️⃣ 현재 캔들 임시 생성 (항상 생성 시도)
                    Candle currentCandle = chartService.recalculateCandleFromExecutions(
                            ticker, interval, currentCandleTime);

                    if (currentCandle != null) {
                        if (currentCandle.getVolume() != null && currentCandle.getVolume().compareTo(BigDecimal.ZERO) > 0) {
                            currentConfirmed++;
                            log.info("[CANDLE/CONFIRM] ✅ Current confirmed: ticker={}, time={}, O={}, H={}, L={}, C={}, V={}",
                                    ticker, currentCandleTime, currentCandle.getOpen(), currentCandle.getHigh(),
                                    currentCandle.getLow(), currentCandle.getClose(), currentCandle.getVolume());
                        } else {
                            currentEmpty++;
                            log.debug("[CANDLE/CONFIRM] 📊 Current empty: ticker={}, time={}, close={}",
                                    ticker, currentCandleTime, currentCandle.getClose());
                        }
                        // ✅ ChartService에서 이미 publishCandle() 호출하므로 여기서 중복 발행 불필요
                    } else {
                        log.debug("[CANDLE/CONFIRM] ⏭️ Skipped: ticker={} (no base price)", ticker);
                    }

                } catch (Exception e) {
                    failedCount++;
                    log.error("[CANDLE/CONFIRM] ❌ Failed: ticker={}", ticker, e);
                }
            }

            log.info("[CANDLE/CONFIRM] ========== Completed: interval={} ==========", interval);
            log.info("[CANDLE/CONFIRM] Previous - confirmed={}, empty={}", previousConfirmed, previousEmpty);
            log.info("[CANDLE/CONFIRM] Current - confirmed={}, empty={}", currentConfirmed, currentEmpty);
            log.info("[CANDLE/CONFIRM] Failed={}", failedCount);

        } catch (Exception e) {
            log.error("[CANDLE/CONFIRM] ❌ Failed: interval={}", interval, e);
        }
    }

    /**
     * 직전 캔들 시작 시각 계산
     *
     * 예: 현재 14:30:00 → 14:29:00 반환 (1분봉)
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
     * 현재 캔들 시작 시각 계산
     *
     * 예: 현재 14:30:00 → 14:30:00 반환 (1분봉)
     */
    private Instant calculateCurrentCandleTime(Instant now, long intervalMinutes) {
        if (intervalMinutes == 1440) {
            return now.atZone(ZoneId.of("UTC"))
                    .truncatedTo(ChronoUnit.DAYS)
                    .toInstant();
        }

        long epochMinutes = now.getEpochSecond() / 60;
        long currentIntervalStart = (epochMinutes / intervalMinutes) * intervalMinutes;

        return Instant.ofEpochSecond(currentIntervalStart * 60);
    }

    /**
     * 활성 종목 수집
     */
    private void updateTrackedTickers() {
        try {
            Set<String> tickers = new HashSet<>();
            int filteredCount = 0;

            // ✅ 개선: Redis에서 활성 종목 추가 수집 (유효성 검증 포함)
            Set<String> candleKeys = redisTemplate.keys("candle:*:*");
            if (candleKeys != null) {
                for (String key : candleKeys) {
                    String[] parts = key.split(":");
                    if (parts.length >= 2) {
                        String ticker = parts[1];
                        if (isValidTicker(ticker)) {
                            tickers.add(ticker);
                        } else {
                            filteredCount++;
                            log.debug("[CANDLE/CONFIRM] Filtered invalid ticker from candle key: {}", ticker);
                        }
                    }
                }
            }

            Set<String> orderBookKeys = redisTemplate.keys("orderbook:*");
            if (orderBookKeys != null) {
                for (String key : orderBookKeys) {
                    String ticker = key.substring("orderbook:".length());
                    if (isValidTicker(ticker)) {
                        tickers.add(ticker);
                    } else {
                        filteredCount++;
                        log.debug("[CANDLE/CONFIRM] Filtered invalid ticker from orderbook key: {}", ticker);
                    }
                }
            }

            Set<String> priceKeys = redisTemplate.keys("price:*");
            if (priceKeys != null) {
                for (String key : priceKeys) {
                    String ticker = key.substring("price:".length());
                    if (isValidTicker(ticker)) {
                        tickers.add(ticker);
                    } else {
                        filteredCount++;
                        log.debug("[CANDLE/CONFIRM] Filtered invalid ticker from price key: {}", ticker);
                    }
                }
            }

            trackedTickers.clear();
            trackedTickers.addAll(tickers);

            log.info("[CANDLE/CONFIRM] Total tracked tickers: {} (filtered: {}) - {}",
                    trackedTickers.size(), filteredCount, trackedTickers);

        } catch (Exception e) {
            log.error("[CANDLE/CONFIRM] Failed to update tickers", e);
        }
    }

    /**
     * ✅ Ticker 유효성 검증
     *
     * 잘못된 ticker 데이터로 인한 CPU 과부하 방지
     * - prev_로 시작하는 메타데이터 키 제외
     * - 콜론(:)이 포함된 비정상 ticker 제외
     * - 정상적인 ticker 패턴만 허용 (영대문자+숫자)
     *
     * @param ticker 검증할 ticker
     * @return 유효하면 true
     */
    private boolean isValidTicker(String ticker) {
        if (ticker == null || ticker.isEmpty()) {
            return false;
        }

        // prev_로 시작하는 메타데이터 키 제외
        if (ticker.startsWith("prev_")) {
            return false;
        }

        // 콜론이 포함된 비정상 ticker 제외 (예: "prev_close:DEPL03")
        if (ticker.contains(":")) {
            return false;
        }

        // 정상적인 ticker 패턴 (숫자 조합, 6자리)
        // "^\\d{6}$" 정규식은 6자리 숫자인지 검증함.
        // 예) 652634
        return ticker.matches("^\\d{6}$");
    }
}