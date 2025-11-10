package com.beyond.MKX.domain.chart.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.chart.dto.MiniChartResDTO;
import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.chart.service.ChartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

        // ✅ start가 null이면 최근 24시간, 아니면 지정된 시간부터
        // 프론트엔드에서 처음 로드 시에는 0 (1970-01-01)을 보내서 전체 데이터 조회
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

    /**
     * 여러 종목의 24시간 종가 데이터 조회 (미니차트용)
     *
     * 1시간봉 기준으로 최근 24시간의 종가를 반환
     * 여러 종목을 한번의 DB 조회로 가져와 성능 최적화
     *
     * @param tickerList 종목 코드 리스트
     * @return 종목별 24시간 종가 데이터 리스트
     */
    @PostMapping("/mini")
    public ResponseEntity<?> getMiniCharts(@RequestBody List<String> tickerList) {

        log.info("미니차트 데이터 조회 요청: {} 종목", tickerList.size());

        if (tickerList == null || tickerList.isEmpty()) {
            return ApiResponse.ok(Collections.emptyList(), "조회할 종목이 없습니다");
        }

        List<MiniChartResDTO> miniCharts = chartService.get24HourClosesForTickers(tickerList);

        log.info("미니차트 데이터 조회 완료: {} 종목", miniCharts.size());

        return ApiResponse.ok(miniCharts, "미니차트 데이터 조회 성공");
    }

    /**
     * 기업용 차트 데이터 조회 (최신 데이터 + 과거 데이터 모두 포함)
     *
     * 기존 /{ticker} 엔드포인트는 start가 null이면 최근 24시간만 반환하지만,
     * 이 엔드포인트는 start와 end를 필수로 받아 해당 기간의 모든 데이터를 반환합니다.
     *
     * @param ticker 종목코드
     * @param interval 캔들 간격 (1m, 5m, 15m, 30m, 1h, 4h, 1d)
     * @param start 시작 시간 (Unix timestamp in milliseconds) - 필수
     * @param end 종료 시간 (Unix timestamp in milliseconds) - 필수
     * @return 캔들 데이터 리스트
     */
    @GetMapping("/{ticker}/corporation")
    public ResponseEntity<?> getCandlesForCorporation(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1d") String interval,
            @RequestParam Long start,
            @RequestParam Long end) {

        log.info("기업용 차트 데이터 조회 요청: ticker={}, interval={}, start={}, end={}",
                ticker, interval, start, end);

        if (start == null || end == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status_message", "start와 end 파라미터는 필수입니다.",
                    "status_code", 400
            ));
        }

        Instant startTime = Instant.ofEpochMilli(start);
        Instant endTime = Instant.ofEpochMilli(end);

        log.info("기업용 차트 데이터 조회 - 변환된 시간: startTime={}, endTime={}, 기간={}일",
                startTime, endTime, java.time.Duration.between(startTime, endTime).toDays());

        List<Candle> candles = chartService.getCandles(ticker, interval, startTime, endTime);

        log.info("기업용 차트 데이터 조회 완료: ticker={}, interval={}, count={}",
                ticker, interval, candles.size());

        if (candles.size() > 0) {
            log.info("기업용 차트 데이터 샘플 - 첫 번째: time={}, close={}, 마지막: time={}, close={}",
                    candles.get(0).getTime(), candles.get(0).getClose(),
                    candles.get(candles.size() - 1).getTime(), candles.get(candles.size() - 1).getClose());
        }

        return ApiResponse.ok(candles, "기업용 차트 데이터 조회 성공");
    }
}
