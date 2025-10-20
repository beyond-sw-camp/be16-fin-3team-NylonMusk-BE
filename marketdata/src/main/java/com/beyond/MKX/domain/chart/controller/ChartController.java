package com.beyond.MKX.domain.chart.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.chart.service.ChartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * 차트 데이터 REST API Controller
 * 
 * OHLCV 캔들스틱 차트 데이터 조회 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/market/chart")
@RequiredArgsConstructor
public class ChartController {

    private final ChartService chartService;

    /**
     * 특정 종목의 캔들 데이터 조회
     * 
     * @param ticker 종목코드
     * @param interval 캔들 간격 (1m, 5m, 15m, 30m, 1h, 4h, 1d)
     * @param start 시작 시간 (Unix timestamp in milliseconds)
     * @param end 종료 시간 (Unix timestamp in milliseconds)
     * @return 캔들 데이터 리스트
     */
    @GetMapping("/{ticker}")
    public ResponseEntity<?> getCandles(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end) {
        
        log.info("캔들 데이터 조회 요청: ticker={}, interval={}, start={}, end={}", 
                ticker, interval, start, end);
        
        // 기본값 설정: 최근 24시간
        Instant startTime = start != null ? Instant.ofEpochMilli(start) : Instant.now().minusSeconds(86400);
        Instant endTime = end != null ? Instant.ofEpochMilli(end) : Instant.now();
        
        List<Candle> candles = chartService.getCandles(ticker, interval, startTime, endTime);
        
        return ApiResponse.ok(candles, "캔들 데이터 조회 성공");
    }

    /**
     * 특정 종목의 최신 캔들 데이터 조회
     * 
     * @param ticker 종목코드
     * @param interval 캔들 간격
     * @return 최신 캔들 데이터
     */
    @GetMapping("/{ticker}/latest")
    public ResponseEntity<?> getLatestCandle(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1m") String interval) {
        
        log.info("최신 캔들 데이터 조회 요청: ticker={}, interval={}", ticker, interval);
        
        Candle candle = chartService.getLatestCandle(ticker, interval);
        
        if (candle == null) {
            return ApiResponse.ok(null, "캔들 데이터가 없습니다");
        }
        
        return ApiResponse.ok(candle, "최신 캔들 데이터 조회 성공");
    }
}
