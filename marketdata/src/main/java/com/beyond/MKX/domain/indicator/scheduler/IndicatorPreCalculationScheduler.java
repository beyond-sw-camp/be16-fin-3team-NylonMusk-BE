package com.beyond.MKX.domain.indicator.scheduler;

import com.beyond.MKX.domain.indicator.dto.IndicatorRequestDTO;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import com.beyond.MKX.domain.indicator.enums.IndicatorType;
import com.beyond.MKX.domain.indicator.service.IndicatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * 보조지표 사전 계산 스케줄러
 * 
 * 주요 종목/지표를 주기적으로 사전 계산하여 캐싱
 * - 1분마다 실행 (설정 가능)
 * - 주요 종목 리스트에 대해 기본 지표 사전 계산
 * - 캐시 워밍업 효과
 * 
 * application.yml에서 활성화:
 * indicator.scheduler.enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "indicator.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class IndicatorPreCalculationScheduler {

    private final IndicatorService indicatorService;

    // 사전 계산할 주요 종목 리스트
    private static final List<String> MAJOR_TICKERS = Arrays.asList(
            "005930",  // 삼성전자
            "000660",  // SK하이닉스
            "035420",  // NAVER
            "005380",  // 현대차
            "051910"   // LG화학
    );

    // 사전 계산할 기본 지표 리스트
    private static final List<IndicatorType> BASIC_INDICATORS = Arrays.asList(
            IndicatorType.MA,
            IndicatorType.EMA,
            IndicatorType.VOLUME,
            IndicatorType.RSI,
            IndicatorType.MACD
    );

    // 사전 계산할 캔들 간격
    private static final List<String> INTERVALS = Arrays.asList("1m", "5m", "15m", "1h", "1d");

    /**
     * 매 1분마다 주요 지표 사전 계산
     * 
     * 실행 시간: 매 분마다 (cron: 0 * * * * ?)
     */
    @Scheduled(cron = "0 * * * * ?")
    @SchedulerLock(name = "preCalculateIndicators", lockAtMostFor = "50000", lockAtLeastFor = "30000")
    public void preCalculateIndicators() {
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int failCount = 0;

        log.info("[SCHEDULER] 🚀 Starting pre-calculation for {} tickers, {} intervals, {} indicators",
                MAJOR_TICKERS.size(), INTERVALS.size(), BASIC_INDICATORS.size());

        Instant now = Instant.now();
        Instant start = now.minusSeconds(86400); // 최근 24시간

        for (String ticker : MAJOR_TICKERS) {
            for (String interval : INTERVALS) {
                for (IndicatorType indicatorType : BASIC_INDICATORS) {
                    try {
                        // 지표 계산 요청 생성
                        IndicatorRequestDTO request = IndicatorRequestDTO.builder()
                                .ticker(ticker)
                                .interval(interval)
                                .indicatorType(indicatorType)
                                .params(null) // 기본 파라미터 사용
                                .build();

                        // 계산 및 캐싱
                        IndicatorResultDTO result = indicatorService.calculateIndicator(request, start, now);

                        if (result != null && result.getDataPointCount() > 0) {
                            successCount++;
                            log.debug("[SCHEDULER] ✅ Pre-calculated: ticker={}, interval={}, type={}, points={}",
                                    ticker, interval, indicatorType, result.getDataPointCount());
                        }

                    } catch (Exception e) {
                        failCount++;
                        log.warn("[SCHEDULER] ⚠️ Failed to pre-calculate: ticker={}, interval={}, type={}",
                                ticker, interval, indicatorType, e.getMessage());
                    }
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[SCHEDULER] ✅ Pre-calculation completed: success={}, fail={}, duration={}ms",
                successCount, failCount, duration);
    }

    /**
     * 매 시간마다 캐시 정리
     * 
     * 실행 시간: 매 시간 정각 (cron: 0 0 * * * ?)
     */
    @Scheduled(cron = "0 0 * * * ?")
    @SchedulerLock(name = "cleanupExpiredCache", lockAtMostFor = "600000", lockAtLeastFor = "60000")
    public void cleanupExpiredCache() {
        log.info("[SCHEDULER] 🧹 Starting cache cleanup...");

        // Redis의 만료된 캐시는 자동으로 삭제되므로
        // 여기서는 통계만 로깅
        for (String ticker : MAJOR_TICKERS) {
            try {
                Map<String, Object> stats = indicatorService.getCacheStats(ticker);
                log.info("[SCHEDULER] Cache stats for {}: {}", ticker, stats);
            } catch (Exception e) {
                log.error("[SCHEDULER] Failed to get cache stats for {}", ticker, e);
            }
        }

        log.info("[SCHEDULER] ✅ Cache cleanup completed");
    }

    /**
     * 매일 자정에 오래된 계산 상태 정리
     * 
     * 실행 시간: 매일 00:00 (cron: 0 0 0 * * ?)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @SchedulerLock(name = "cleanupOldStates", lockAtMostFor = "1800000", lockAtLeastFor = "300000")
    public void cleanupOldStates() {
        log.info("[SCHEDULER] 🗑️ Starting old states cleanup...");

        // 전체 종목 캐시 무효화 (24시간 이상 경과한 데이터)
        int cleanedCount = 0;

        for (String ticker : MAJOR_TICKERS) {
            try {
                indicatorService.invalidateAllCacheForTicker(ticker);
                cleanedCount++;
                log.debug("[SCHEDULER] Invalidated all cache for ticker: {}", ticker);
            } catch (Exception e) {
                log.error("[SCHEDULER] Failed to invalidate cache for {}", ticker, e);
            }
        }

        log.info("[SCHEDULER] ✅ Old states cleanup completed: cleaned {} tickers", cleanedCount);
    }

//    /**
//     * 매 10분마다 스케줄러 상태 체크
//     *
//     * 실행 시간: 매 10분 (cron: 0 */10 * * * ?)
//     */
    @Scheduled(cron = "0 */10 * * * ?")
    @SchedulerLock(name = "indicatorHealthCheck", lockAtMostFor = "60000", lockAtLeastFor = "10000")
    public void healthCheck() {
        log.debug("[SCHEDULER] 💓 Health check: Scheduler is running normally");

        // 간단한 계산 테스트
        try {
            String testTicker = MAJOR_TICKERS.get(0);
            IndicatorRequestDTO request = IndicatorRequestDTO.createMARequest(testTicker, "1d", 20);

            IndicatorResultDTO result = indicatorService.calculateIndicator(
                    request,
                    Instant.now().minusSeconds(86400),
                    Instant.now()
            );

            if (result != null && result.getDataPointCount() > 0) {
                log.debug("[SCHEDULER] ✅ Health check passed: dataPoints={}", result.getDataPointCount());
            } else {
                log.warn("[SCHEDULER] ⚠️ Health check warning: empty result");
            }

        } catch (Exception e) {
            log.error("[SCHEDULER] ❌ Health check failed", e);
        }
    }
}
