package com.beyond.MKX.domain.market.scheduler;

import com.beyond.MKX.domain.market.service.Week52RangeService;
import com.beyond.MKX.domain.price.service.CurrentPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

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
     * 전일 거래량 저장 (매일 자정에 실행)
     * 
     * 장 마감 후 현재 거래량을 전일 거래량으로 저장
     * 다음 거래일의 거래량 변화율 계산에 사용
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    public void savePreviousDayVolumes() {
        try {
            Set<String> priceKeys = redisTemplate.keys("price:*");
            
            if (priceKeys != null && !priceKeys.isEmpty()) {
                int savedCount = 0;
                
                for (String key : priceKeys) {
                    if (!key.startsWith("price:prev_")) {
                        String ticker = key.replace("price:", "");
                        
                        try {
                            currentPriceService.savePrevVolume(ticker);
                            savedCount++;
                        } catch (Exception e) {
                            log.warn("Failed to save prev volume for ticker: {}", ticker, e);
                        }
                    }
                }
                
                log.info("Saved previous day volumes for {} tickers", savedCount);
            }
            
        } catch (Exception e) {
            log.error("Failed to save previous day volumes", e);
        }
    }
    
    /**
     * 전일 종가 저장 (매일 오후 6시에 실행 - 장 마감 시간에 맞춰 조정 가능)
     * 
     * 현재가를 전일 종가로 저장하여 다음 거래일 등락률 계산에 사용
     */
    @Scheduled(cron = "0 0 18 * * *") // 매일 오후 6시
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
