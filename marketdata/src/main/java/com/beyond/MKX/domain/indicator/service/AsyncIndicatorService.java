package com.beyond.MKX.domain.indicator.service;

import com.beyond.MKX.domain.indicator.dto.IndicatorRequestDTO;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 비동기 지표 계산 서비스
 * 
 * 여러 지표를 동시에 계산하여 성능 향상
 * @Async를 사용한 병렬 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncIndicatorService {

    private final IndicatorService indicatorService;

    /**
     * 단일 지표 비동기 계산
     */
    @Async("indicatorTaskExecutor")
    public CompletableFuture<IndicatorResultDTO> calculateAsync(
            IndicatorRequestDTO request, Instant start, Instant end) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("[ASYNC-INDICATOR] Starting async calculation: type={}, ticker={}", 
                    request.getIndicatorType(), request.getTicker());
            
            IndicatorResultDTO result = indicatorService.calculateIndicator(request, start, end);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[ASYNC-INDICATOR] ✅ Completed: type={}, duration={}ms", 
                    request.getIndicatorType(), duration);
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            log.error("[ASYNC-INDICATOR] ❌ Failed: type={}", request.getIndicatorType(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 여러 지표 병렬 계산
     * 
     * 모든 지표를 동시에 계산하여 전체 처리 시간 단축
     */
    public CompletableFuture<List<IndicatorResultDTO>> calculateMultipleAsync(
            String ticker, String interval, 
            List<IndicatorRequestDTO> requests, 
            Instant start, Instant end) {
        
        long overallStartTime = System.currentTimeMillis();
        
        log.info("[ASYNC-INDICATOR] 🚀 Starting parallel calculation: {} indicators for ticker={}", 
                requests.size(), ticker);
        
        // 모든 지표를 병렬로 계산
        List<CompletableFuture<IndicatorResultDTO>> futures = new ArrayList<>();
        
        for (IndicatorRequestDTO request : requests) {
            CompletableFuture<IndicatorResultDTO> future = calculateAsync(request, start, end);
            futures.add(future);
        }
        
        // 모든 계산이 완료될 때까지 대기
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<IndicatorResultDTO> results = new ArrayList<>();
                    
                    for (CompletableFuture<IndicatorResultDTO> future : futures) {
                        try {
                            results.add(future.get());
                        } catch (Exception e) {
                            log.error("[ASYNC-INDICATOR] Failed to get result from future", e);
                        }
                    }
                    
                    long overallDuration = System.currentTimeMillis() - overallStartTime;
                    log.info("[ASYNC-INDICATOR] ✅ Completed all {} indicators in {}ms (avg: {}ms)", 
                            results.size(), overallDuration, overallDuration / Math.max(1, results.size()));
                    
                    return results;
                });
    }

    /**
     * 지표 계산 상태 체크
     */
    @Async("indicatorTaskExecutor")
    public CompletableFuture<Boolean> checkCalculationHealth(String ticker, String interval) {
        try {
            log.debug("[ASYNC-INDICATOR] Checking calculation health: ticker={}, interval={}", 
                    ticker, interval);
            
            // 간단한 MA 계산으로 시스템 상태 체크
            IndicatorRequestDTO testRequest = IndicatorRequestDTO.createMARequest(ticker, interval, 20);
            IndicatorResultDTO result = indicatorService.calculateIndicator(
                    testRequest, 
                    Instant.now().minusSeconds(86400), 
                    Instant.now()
            );
            
            boolean isHealthy = result != null && result.getDataPointCount() > 0;
            
            log.debug("[ASYNC-INDICATOR] Health check result: {}", isHealthy ? "✅ OK" : "❌ Failed");
            
            return CompletableFuture.completedFuture(isHealthy);
            
        } catch (Exception e) {
            log.error("[ASYNC-INDICATOR] Health check failed", e);
            return CompletableFuture.completedFuture(false);
        }
    }
}
