package com.beyond.MKX.domain.orderbook.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.orderbook.dto.enhanced.EnhancedOrderBookDTO;
import com.beyond.MKX.domain.orderbook.entity.OrderBook;
import com.beyond.MKX.domain.orderbook.service.EnhancedOrderBookService;
import com.beyond.MKX.domain.orderbook.service.OrderBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 호가 데이터 REST API 컨트롤러 (확장)
 * 
 * 기존 호가 조회 + 고도화된 호가 조회 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orderbook")
@RequiredArgsConstructor
public class EnhancedOrderBookController {
    
    private final OrderBookService orderBookService;
    private final EnhancedOrderBookService enhancedOrderBookService;
    
    /**
     * 기본 호가 조회 (기존 API 유지)
     * 
     * GET /api/v1/orderbook/{ticker}
     */
    @GetMapping("/{ticker}")
    public ResponseEntity<?> getOrderBook(@PathVariable String ticker) {
        log.info("[API/ORDERBOOK] Request: GET /orderbook/{}", ticker);
        
        OrderBook orderBook = orderBookService.getOrderBook(ticker);
        
        if (orderBook == null || (orderBook.getBids().isEmpty() && orderBook.getAsks().isEmpty())) {
            return ApiResponse.ok(OrderBook.createEmpty(ticker), "No orderbook data available");
        }
        
        return ApiResponse.ok(orderBook, "Orderbook retrieved successfully");
    }
    
    /**
     * 고도화된 호가 조회 (신규 API)
     * 
     * GET /api/v1/orderbook/{ticker}/enhanced
     * 
     * 응답: 기본 호가 + 시장 요약 + 통계 정보
     */
    @GetMapping("/{ticker}/enhanced")
    public ResponseEntity<?> getEnhancedOrderBook(@PathVariable String ticker) {
        log.info("[API/ORDERBOOK-ENHANCED] Request: GET /orderbook/{}/enhanced", ticker);
        
        EnhancedOrderBookDTO enhancedData = enhancedOrderBookService.getEnhancedOrderBook(ticker);
        
        return ApiResponse.ok(enhancedData, "Enhanced orderbook retrieved successfully");
    }
//
//    /**
//     * WebSocket 세션 수 조회 (디버깅용)
//     *
//     * GET /api/v1/orderbook/{ticker}/sessions
//     */
//    @GetMapping("/{ticker}/sessions")
//    public ResponseEntity<?> getSessionCount(@PathVariable String ticker) {
//        int count = orderBookService.getWebSocketSessionCount(ticker);
//
//        return ApiResponse.ok(count, "WebSocket session count retrieved");
//    }
}
