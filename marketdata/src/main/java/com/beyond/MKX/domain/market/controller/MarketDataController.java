package com.beyond.MKX.domain.market.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.market.dto.MarketDataDTO;
import com.beyond.MKX.domain.orderbook.entity.OrderBook;
import com.beyond.MKX.domain.orderbook.service.OrderBookService;
import com.beyond.MKX.domain.price.entity.CurrentPrice;
import com.beyond.MKX.domain.price.service.CurrentPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

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
    
    private final CurrentPriceService currentPriceService;
    private final OrderBookService orderBookService;
    
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
            // 1. 현재가 조회
            CurrentPrice currentPrice = currentPriceService.getCurrentPrice(ticker);
            
            // 2. 호가 조회
            OrderBook orderBook = orderBookService.getOrderBook(ticker);
            
            // 3. 사용자 주문 조회 (Optional - 향후 구현)
            // List<Long> userOrders = null;
            // if (userId != null) {
            //     userOrders = userOrderService.getUserOrderPrices(ticker, userId);
            // }
            
            // 4. DTO 조합 (초기 데이터 없을 경우 0으로 설정)
            MarketDataDTO marketData = MarketDataDTO.builder()
                .currentPrice(currentPrice != null ? currentPrice.getPrice() : 0L)
                .prevClose(currentPrice != null ? currentPrice.getPrevClose() : 0L)
                .open(currentPrice != null ? currentPrice.getOpen() : 0L)
                .high(currentPrice != null ? currentPrice.getHigh() : 0L)
                .low(currentPrice != null ? currentPrice.getLow() : 0L)
                .change(currentPrice != null ? currentPrice.getChange() : 0L)
                .changeRate(currentPrice != null && currentPrice.getChangeRate() != null ? 
                    currentPrice.getChangeRate() : BigDecimal.ZERO)
                .volume(currentPrice != null && currentPrice.getVolume() != null ? 
                    currentPrice.getVolume() : BigDecimal.ZERO)
                .volumeChange(currentPrice != null && currentPrice.getVolumeChange() != null ? 
                    currentPrice.getVolumeChange() : BigDecimal.ZERO)
                .prevVolume(currentPrice != null && currentPrice.getPrevVolume() != null ?
                    currentPrice.getPrevVolume() : BigDecimal.ZERO)
                .week52High(currentPrice != null ? currentPrice.getWeek52High() : 0L)
                .week52Low(currentPrice != null ? currentPrice.getWeek52Low() : 0L)
                .executionStrength(currentPrice != null && currentPrice.getExecutionStrength() != null ? 
                    currentPrice.getExecutionStrength() : BigDecimal.ZERO)
                .bids(orderBook != null ? orderBook.getBids() : new ArrayList<>())
                .asks(orderBook != null ? orderBook.getAsks() : new ArrayList<>())
                .userOrders(null) // 향후 구현
                .timestamp(Instant.now())
                .build();
            
            log.debug("통합 마켓 데이터 조회 완료: ticker={}, price={}, bids={}, asks={}", 
                ticker, marketData.getCurrentPrice(), 
                marketData.getBids().size(), marketData.getAsks().size());
            
            return ApiResponse.ok(marketData, "통합 마켓 데이터 조회 성공");
            
        } catch (Exception e) {
            log.error("통합 마켓 데이터 조회 실패: ticker={}", ticker, e);
            return ApiResponse.noContent("통합 마켓 데이터 조회 실패: " + e.getMessage());
        }
    }
}
