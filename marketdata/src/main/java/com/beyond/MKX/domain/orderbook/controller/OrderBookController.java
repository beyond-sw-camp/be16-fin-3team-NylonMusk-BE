package com.beyond.MKX.domain.orderbook.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.orderbook.entity.OrderBook;
import com.beyond.MKX.domain.orderbook.service.OrderBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 호가 데이터 REST API Controller
 * 
 * 실시간 호가 정보 조회 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/market/orderbook")
@RequiredArgsConstructor
public class OrderBookController {

    private final OrderBookService orderBookService;

    /**
     * 특정 종목의 호가 정보 조회
     * 
     * @param ticker 종목코드
     * @return 호가 데이터 (매수호가, 매도호가)
     */
    @GetMapping("/{ticker}")
    public ResponseEntity<?> getOrderbook(@PathVariable String ticker) {
        log.info("호가 조회 요청: ticker={}", ticker);
        
        OrderBook orderbook = orderBookService.getOrderBook(ticker);
        
        return ApiResponse.ok(orderbook, "호가 조회 성공");
    }

    /**
     * 호가 초기화 (테스트용)
     * 
     * @param ticker 종목코드
     */
    @PostMapping("/{ticker}/initialize")
    public ResponseEntity<?> initializeOrderbook(@PathVariable String ticker) {
        log.info("호가 초기화 요청: ticker={}", ticker);
        
        orderBookService.initializeOrderBook(ticker);
        
        return ApiResponse.ok(null, "호가 초기화 성공");
    }

    /**
     * 매수 호가 추가 (테스트용)
     * 
     * @param ticker 종목코드
     * @param price 호가 가격
     * @param quantity 호가 수량
     */
    @PostMapping("/{ticker}/bid")
    public ResponseEntity<?> addBid(
            @PathVariable String ticker,
            @RequestParam long price,
            @RequestParam BigDecimal quantity) {
        
        log.info("매수 호가 추가 요청: ticker={}, price={}, quantity={}", ticker, price, quantity);
        
        orderBookService.addBid(ticker, price, quantity);
        
        return ApiResponse.ok(null, "매수 호가 추가 성공");
    }

    /**
     * 매도 호가 추가 (테스트용)
     * 
     * @param ticker 종목코드
     * @param price 호가 가격
     * @param quantity 호가 수량
     */
    @PostMapping("/{ticker}/ask")
    public ResponseEntity<?> addAsk(
            @PathVariable String ticker,
            @RequestParam long price,
            @RequestParam BigDecimal quantity) {
        
        log.info("매도 호가 추가 요청: ticker={}, price={}, quantity={}", ticker, price, quantity);
        
        orderBookService.addAsk(ticker, price, quantity);
        
        return ApiResponse.ok(null, "매도 호가 추가 성공");
    }

    /**
     * 호가 삭제 (테스트용)
     * 
     * @param ticker 종목코드
     */
    @DeleteMapping("/{ticker}")
    public ResponseEntity<?> deleteOrderbook(@PathVariable String ticker) {
        log.info("호가 삭제 요청: ticker={}", ticker);
        
        orderBookService.deleteOrderBook(ticker);
        
        return ApiResponse.ok(null, "호가 삭제 성공");
    }
    
//    /**
//     * WebSocket 연결 상태 확인 (디버깅용)
//     *
//     * @param ticker 종목코드
//     * @return WebSocket 세션 수
//     */
//    @GetMapping("/{ticker}/sessions")
//    public ResponseEntity<?> getSessionCount(@PathVariable String ticker) {
//        log.info("WebSocket 세션 수 조회: ticker={}", ticker);
//
//        int sessionCount = orderBookService.getWebSocketSessionCount(ticker);
//
//        return ApiResponse.ok(
//            java.util.Map.of(
//                "ticker", ticker,
//                "sessionCount", sessionCount
//            ),
//            "WebSocket 세션 수 조회 성공"
//        );
//    }
}
