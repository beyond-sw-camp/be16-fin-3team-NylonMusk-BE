package com.beyond.MKX.domain.market.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.orderbook.dto.enhanced.EnhancedOrderBookDTO;
import com.beyond.MKX.domain.orderbook.service.EnhancedOrderBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 통합 마켓 데이터 REST API Controller
 * 
 * 호가창 고도화를 위한 통합 API 제공
 * 하나의 API로 현재가, 호가, 52주 정보, 체결강도 모두 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/market/integrated")
@RequiredArgsConstructor
public class MarketDataController {
    
    private final EnhancedOrderBookService enhancedOrderBookService;
    
    /**
     * 통합 마켓 데이터 조회 (호가창 최적화)
     * 
     * 하나의 API로 현재가, 호가, 52주 정보, 체결강도 모두 제공
     * 프론트엔드의 여러 API 호출을 하나로 통합
     * 
     * @param ticker 종목코드
     * @param userId 사용자 ID (Optional - 향후 확장)
     * @return 통합 마켓 데이터
     */
    @GetMapping("/{ticker}")
    public ResponseEntity<?> getIntegratedMarketData(
            @PathVariable String ticker,
            @RequestParam(required = false) Long userId) {
        
        log.info("통합 마켓 데이터 조회: ticker={}, userId={}", ticker, userId);
        
        try {
            // ✅ EnhancedOrderBookService를 사용하여 statistics 포함된 데이터 반환
            EnhancedOrderBookDTO enhancedData = enhancedOrderBookService.getEnhancedOrderBook(ticker);
            
            if (enhancedData == null) {
                log.warn("[INTEGRATED-MARKET] Enhanced data is null for ticker: {}", ticker);
                return ApiResponse.noContent("통합 마켓 데이터를 조회할 수 없습니다");
            }
            
            log.info("[INTEGRATED-MARKET] ✅ 통합 마켓 데이터 조회 완료: ticker={}, bids={}, asks={}, totalBidVolume={}, totalAskVolume={}", 
                ticker, 
                enhancedData.getBids() != null ? enhancedData.getBids().size() : 0,
                enhancedData.getAsks() != null ? enhancedData.getAsks().size() : 0,
                enhancedData.getStatistics() != null ? enhancedData.getStatistics().getTotalBidVolume() : null,
                enhancedData.getStatistics() != null ? enhancedData.getStatistics().getTotalAskVolume() : null);
            
            return ApiResponse.ok(enhancedData, "통합 마켓 데이터 조회 성공");
            
        } catch (Exception e) {
            log.error("통합 마켓 데이터 조회 실패: ticker={}", ticker, e);
            return ApiResponse.noContent("통합 마켓 데이터 조회 실패: " + e.getMessage());
        }
    }
}
