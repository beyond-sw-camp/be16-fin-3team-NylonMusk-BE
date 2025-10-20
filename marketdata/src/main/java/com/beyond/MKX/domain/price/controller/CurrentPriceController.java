package com.beyond.MKX.domain.price.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.price.entity.CurrentPrice;
import com.beyond.MKX.domain.price.service.CurrentPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 현재가 REST API Controller
 * 
 * 실시간 주가 정보 조회 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/market/price")
@RequiredArgsConstructor
public class CurrentPriceController {

    private final CurrentPriceService currentPriceService;

    /**
     * 특정 종목의 현재가 조회
     * 
     * @param ticker 종목코드
     * @return 현재가 정보
     */
    @GetMapping("/{ticker}")
    public ResponseEntity<?> getCurrentPrice(@PathVariable String ticker) {
        log.info("현재가 조회 요청: ticker={}", ticker);
        
        CurrentPrice currentPrice = currentPriceService.getCurrentPrice(ticker);
        
        if (currentPrice == null) {
            return ApiResponse.ok(null, "현재가 정보가 없습니다");
        }
        
        return ApiResponse.ok(currentPrice, "현재가 조회 성공");
    }

    /**
     * 전일 종가 설정 (테스트/관리용)
     * 
     * @param ticker 종목코드
     * @return 성공 여부
     */
    @PostMapping("/{ticker}/prev-close")
    public ResponseEntity<?> setPrevClose(@PathVariable String ticker) {
        log.info("전일 종가 설정 요청: ticker={}", ticker);
        
        currentPriceService.setPrevClosePrice(ticker);
        
        return ApiResponse.ok(null, "전일 종가 설정 완료");
    }

    /**
     * 장 시작 초기화 (테스트/관리용)
     * 
     * @param ticker 종목코드
     * @return 성공 여부
     */
    @PostMapping("/{ticker}/initialize")
    public ResponseEntity<?> initializeDaily(@PathVariable String ticker) {
        log.info("장 시작 초기화 요청: ticker={}", ticker);
        
        currentPriceService.initializeDailyPrice(ticker);
        
        return ApiResponse.ok(null, "장 시작 초기화 완료");
    }
}
