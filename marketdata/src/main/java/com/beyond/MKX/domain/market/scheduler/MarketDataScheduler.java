package com.beyond.MKX.domain.market.scheduler;

import com.beyond.MKX.domain.market.service.Week52RangeService;
import com.beyond.MKX.domain.price.service.CurrentPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 마켓 데이터 주기적 업데이트 스케줄러
 * 
 * 거래량 변화율, 52주 최고/최저가 등 주기적으로 계산이 필요한 데이터를 자동 업데이트
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataScheduler {
    
    private final CurrentPriceService currentPriceService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Week52RangeService week52RangeService;
    
    /**
     * 거래량 변화율 업데이트 (1분마다)
     * 
     * 활성 종목(Redis에 현재가 데이터가 있는 종목)에 대해
     * 거래량 변화율을 계산하고 업데이트
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000) // 1분
    @SchedulerLock(name = "updateVolumeChanges", lockAtMostFor = "50s", lockAtLeastFor = "10s")
    public void updateVolumeChanges() {
        try {
            // Redis에서 활성 종목 목록 조회
            Set<String> priceKeys = redisTemplate.keys("price:*");
            
            if (priceKeys != null && !priceKeys.isEmpty()) {
                int successCount = 0;
                int errorCount = 0;
                
                for (String key : priceKeys) {
                    // "price:" prefix 제거하여 ticker 추출
                    if (!key.startsWith("price:prev_")) {  // prev_close, prev_volume 키 제외
                        String ticker = key.replace("price:", "");
                        
                        try {
                            currentPriceService.updateVolumeChange(ticker);
                            successCount++;
                        } catch (Exception e) {
                            log.warn("Failed to update volume change for ticker: {}", ticker, e);
                            errorCount++;
                        }
                    }
                }
                
                log.debug("Completed volume change update: success={}, error={}", 
                    successCount, errorCount);
            }
            
        } catch (Exception e) {
            log.error("Failed to update volume changes", e);
        }
    }
    
    /**
     * 52주 최고/최저가 업데이트 (5분마다)
     * 
     * 주의: InfluxDB 쿼리는 비용이 크므로 너무 자주 실행하지 않도록 주의
     * 활성 종목(Redis에 현재가 데이터가 있는 종목)에 대해서만 실행
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5분
    @SchedulerLock(name = "update52WeekRanges", lockAtMostFor = "4m50s", lockAtLeastFor = "1m")
    public void update52WeekRanges() {
        try {
            Set<String> priceKeys = redisTemplate.keys("price:*");
            
            if (priceKeys != null && !priceKeys.isEmpty()) {
                int successCount = 0;
                int errorCount = 0;
                
                for (String key : priceKeys) {
                    if (!key.startsWith("price:prev_")) {
                        String ticker = key.replace("price:", "");
                        
                        try {
                            week52RangeService.update52WeekRange(ticker);
                            successCount++;
                        } catch (Exception e) {
                            log.warn("Failed to update 52-week range for ticker: {}", ticker, e);
                            errorCount++;
                        }
                    }
                }
                
                log.info("Completed 52-week range update: success={}, error={}", 
                    successCount, errorCount);
            }
            
        } catch (Exception e) {
            log.error("Failed to update 52-week ranges", e);
        }
    }
    
    /**
     * 매일 자정 장 초기화 프로세스
     * 
     * 실행 순서 (중요!):
     * 1. 전일 종가 저장 (price:prev_close:ticker)
     * 2. 전일 거래량 저장 (price:prev_volume:ticker)
     * 3. 당일 데이터 초기화 (volume=0, prevClose 업데이트)
     * 
     * 주의: 순서가 바뀌면 데이터 정합성 문제 발생 가능
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul") // 매일 자정 (KST)
    @SchedulerLock(name = "initializeAllDailyPrices", lockAtMostFor = "10m", lockAtLeastFor = "2m")
    public void initializeAllDailyPrices() {
        try {
            Set<String> priceKeys = redisTemplate.keys("price:*");
            
            if (priceKeys == null || priceKeys.isEmpty()) {
                log.warn("[DAILY-INIT] No price keys found for daily initialization");
                return;
            }
            
            // price:prev_close, price:prev_volume 키 제외하고 ticker 추출
            List<String> tickers = priceKeys.stream()
                    .filter(key -> !key.startsWith("price:prev_"))
                    .map(key -> key.replace("price:", ""))
                    .collect(Collectors.toList());
            
            log.info("[DAILY-INIT] ========== Starting daily initialization for {} tickers ==========", 
                    tickers.size());
            
            int step1Success = 0, step2Success = 0, step3Success = 0;
            int step1Fail = 0, step2Fail = 0, step3Fail = 0;
            
            for (String ticker : tickers) {
                try {
                    // STEP 1: 전일 종가 저장 (현재가 → price:prev_close:ticker)
                    currentPriceService.setPrevClosePrice(ticker);
                    step1Success++;
                    
                } catch (Exception e) {
                    log.error("[DAILY-INIT] Failed to save prev close for ticker: {}", ticker, e);
                    step1Fail++;
                }
                
                try {
                    // STEP 2: 전일 거래량 저장 (현재 거래량 → price:prev_volume:ticker)
                    currentPriceService.savePrevVolume(ticker);
                    step2Success++;
                    
                } catch (Exception e) {
                    log.error("[DAILY-INIT] Failed to save prev volume for ticker: {}", ticker, e);
                    step2Fail++;
                }
                
                try {
                    // STEP 3: 당일 데이터 초기화 (volume=0, prevClose 업데이트)
                    currentPriceService.initializeDailyPrice(ticker);
                    step3Success++;
                    
                } catch (Exception e) {
                    log.error("[DAILY-INIT] Failed to initialize daily price for ticker: {}", ticker, e);
                    step3Fail++;
                }
            }
            
            log.info("[DAILY-INIT] ========== Completed daily initialization ==========");
            log.info("[DAILY-INIT] STEP 1 (Prev Close): success={}, fail={}", step1Success, step1Fail);
            log.info("[DAILY-INIT] STEP 2 (Prev Volume): success={}, fail={}", step2Success, step2Fail);
            log.info("[DAILY-INIT] STEP 3 (Initialize): success={}, fail={}", step3Success, step3Fail);
            log.info("[DAILY-INIT] ========================================================");
            
        } catch (Exception e) {
            log.error("[DAILY-INIT] Critical error during daily initialization", e);
        }
    }
    
    /**
     * @deprecated 이제 initializeAllDailyPrices()에 통합되어 자정에 자동 실행됨
     * 
     * 전일 종가 저장 (매일 오후 6시에 실행 - 장 마감 시간에 맞춰 조정 가능)
     * 현재가를 전일 종가로 저장하여 다음 거래일 등락률 계산에 사용
     */
    // @Scheduled(cron = "0 0 18 * * *") // 비활성화 - initializeAllDailyPrices()로 통합
    @Deprecated
    public void savePreviousDayClosePrices() {
        try {
            Set<String> priceKeys = redisTemplate.keys("price:*");
            
            if (priceKeys != null && !priceKeys.isEmpty()) {
                int savedCount = 0;
                
                for (String key : priceKeys) {
                    if (!key.startsWith("price:prev_")) {
                        String ticker = key.replace("price:", "");
                        
                        try {
                            currentPriceService.setPrevClosePrice(ticker);
                            savedCount++;
                        } catch (Exception e) {
                            log.warn("Failed to save prev close price for ticker: {}", ticker, e);
                        }
                    }
                }
                
                log.info("Saved previous day close prices for {} tickers", savedCount);
            }
            
        } catch (Exception e) {
            log.error("Failed to save previous day close prices", e);
        }
    }
}
