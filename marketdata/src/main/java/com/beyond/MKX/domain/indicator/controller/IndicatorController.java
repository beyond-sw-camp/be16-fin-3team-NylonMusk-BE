package com.beyond.MKX.domain.indicator.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.indicator.dto.IndicatorRequestDTO;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import com.beyond.MKX.domain.indicator.dto.UserIndicatorConfigDTO;
import com.beyond.MKX.domain.indicator.enums.IndicatorType;
import com.beyond.MKX.domain.indicator.service.IndicatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 보조지표 REST API Controller
 * 
 * 보조지표 계산 및 사용자 설정 관리 API 제공
 * 
 * ⚠️ 변경사항: IndicatorWebSocketHandler 제거
 * - Native WebSocket Handler 사용 중단
 * - IndicatorStompController가 STOMP를 통해 실시간 데이터 전송
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/market/indicator")
@RequiredArgsConstructor
public class IndicatorController {

    private final IndicatorService indicatorService;
    // ❌ indicatorWebSocketHandler 제거: STOMP로 대체됨

    /**
     * 단일 보조지표 계산 및 조회
     * 
     * @param ticker 종목코드
     * @param interval 캔들 간격
     * @param indicatorType 지표 타입
     * @param params 지표 파라미터 (JSON)
     * @param start 시작 시간 (Unix timestamp in milliseconds)
     * @param end 종료 시간 (Unix timestamp in milliseconds)
     * @return 계산된 지표 데이터
     */
    @GetMapping("/{ticker}/{indicatorType}")
    public ResponseEntity<?> getIndicator(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1m") String interval,
            @PathVariable IndicatorType indicatorType,
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end) {
        
        log.info("보조지표 계산 요청: ticker={}, interval={}, indicatorType={}, params={}", 
                ticker, interval, indicatorType, params);
        
        // 기본값 설정: 최근 24시간
        Instant startTime = start != null ? Instant.ofEpochMilli(start) : Instant.now().minusSeconds(86400);
        Instant endTime = end != null ? Instant.ofEpochMilli(end) : Instant.now();
        
        // 요청 DTO 생성
        IndicatorRequestDTO request = IndicatorRequestDTO.builder()
                .ticker(ticker)
                .interval(interval)
                .indicatorType(indicatorType)
                .params(params)
                .build();
        
        // 지표 계산
        IndicatorResultDTO result = indicatorService.calculateIndicator(request, startTime, endTime);
        
        // ❌ WebSocket 전송 제거: IndicatorStompController가 이벤트를 통해 처리
        // indicatorWebSocketHandler.broadcastIndicator(ticker, result);
        
        return ApiResponse.ok(result, "보조지표 계산 완료");
    }

    /**
     * 여러 보조지표 일괄 계산
     * 
     * @param ticker 종목코드
     * @param interval 캔들 간격
     * @param requests 지표 요청 리스트
     * @param start 시작 시간
     * @param end 종료 시간
     * @return 계산된 지표 데이터 리스트
     */
    @PostMapping("/{ticker}/batch")
    public ResponseEntity<?> getMultipleIndicators(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestBody List<IndicatorRequestDTO> requests,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end) {
        
        log.info("여러 보조지표 계산 요청: ticker={}, interval={}, count={}", 
                ticker, interval, requests.size());
        
        Instant startTime = start != null ? Instant.ofEpochMilli(start) : Instant.now().minusSeconds(86400);
        Instant endTime = end != null ? Instant.ofEpochMilli(end) : Instant.now();
        
        List<IndicatorResultDTO> results = indicatorService.calculateMultipleIndicators(
                ticker, interval, requests, startTime, endTime);
        
        // ❌ WebSocket 전송 제거: STOMP로 대체됨
        // for (IndicatorResultDTO result : results) {
        //     indicatorWebSocketHandler.broadcastIndicator(ticker, result);
        // }
        
        return ApiResponse.ok(results, "보조지표 일괄 계산 완료");
    }

    /**
     * 사용자 지표 설정 저장 (ON/OFF)
     * 
     * @param config 사용자 지표 설정
     * @return 성공 메시지
     */
    @PostMapping("/config")
    public ResponseEntity<?> saveIndicatorConfig(@RequestBody UserIndicatorConfigDTO config) {
        log.info("사용자 지표 설정 저장 요청: userId={}, ticker={}, indicator={}, enabled={}", 
                config.getUserId(), config.getTicker(), config.getIndicatorType(), config.isEnabled());
        
        indicatorService.saveUserIndicatorConfig(config);
        
        return ApiResponse.ok(null, "사용자 지표 설정 저장 완료");
    }

    /**
     * 사용자의 활성화된 지표 목록 조회
     * 
     * @param userId 사용자 ID
     * @param ticker 종목코드
     * @param interval 캔들 간격
     * @return 활성화된 지표 목록
     */
    @GetMapping("/config/{userId}/{ticker}")
    public ResponseEntity<?> getEnabledIndicators(
            @PathVariable String userId,
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1m") String interval) {
        
        log.info("사용자 활성화 지표 조회: userId={}, ticker={}, interval={}", 
                userId, ticker, interval);
        
        List<UserIndicatorConfigDTO> configs = indicatorService.getEnabledIndicators(userId, ticker, interval);
        
        return ApiResponse.ok(configs, "활성화된 지표 목록 조회 완료");
    }

    /**
     * 사용자 지표 설정 삭제
     * 
     * @param userId 사용자 ID
     * @param ticker 종목코드
     * @param interval 캔들 간격
     * @param indicatorType 지표 타입
     * @return 성공 메시지
     */
    @DeleteMapping("/config/{userId}/{ticker}/{indicatorType}")
    public ResponseEntity<?> deleteIndicatorConfig(
            @PathVariable String userId,
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1m") String interval,
            @PathVariable IndicatorType indicatorType) {
        
        log.info("사용자 지표 설정 삭제: userId={}, ticker={}, indicator={}", 
                userId, ticker, indicatorType);
        
        indicatorService.deleteUserIndicatorConfig(userId, ticker, interval, indicatorType);
        
        return ApiResponse.ok(null, "사용자 지표 설정 삭제 완료");
    }

    /**
     * 지원하는 보조지표 목록 조회
     * 
     * @return 지표 타입 목록
     */
    @GetMapping("/types")
    public ResponseEntity<?> getSupportedIndicators() {
        log.info("지원 보조지표 목록 조회");
        
        List<Map<String, Object>> indicators = List.of(
                Map.of(
                        "type", IndicatorType.MA,
                        "name", IndicatorType.MA.getDisplayName(),
                        "position", IndicatorType.MA.getPosition(),
                        "defaultEnabled", IndicatorType.MA.isDefaultEnabled()
                ),
                Map.of(
                        "type", IndicatorType.EMA,
                        "name", IndicatorType.EMA.getDisplayName(),
                        "position", IndicatorType.EMA.getPosition(),
                        "defaultEnabled", IndicatorType.EMA.isDefaultEnabled()
                ),
                Map.of(
                        "type", IndicatorType.RSI,
                        "name", IndicatorType.RSI.getDisplayName(),
                        "position", IndicatorType.RSI.getPosition(),
                        "defaultEnabled", IndicatorType.RSI.isDefaultEnabled()
                ),
                Map.of(
                        "type", IndicatorType.MACD,
                        "name", IndicatorType.MACD.getDisplayName(),
                        "position", IndicatorType.MACD.getPosition(),
                        "defaultEnabled", IndicatorType.MACD.isDefaultEnabled()
                ),
                Map.of(
                        "type", IndicatorType.BOLLINGER_BANDS,
                        "name", IndicatorType.BOLLINGER_BANDS.getDisplayName(),
                        "position", IndicatorType.BOLLINGER_BANDS.getPosition(),
                        "defaultEnabled", IndicatorType.BOLLINGER_BANDS.isDefaultEnabled()
                ),
                Map.of(
                        "type", IndicatorType.VOLUME,
                        "name", IndicatorType.VOLUME.getDisplayName(),
                        "position", IndicatorType.VOLUME.getPosition(),
                        "defaultEnabled", IndicatorType.VOLUME.isDefaultEnabled()
                )
        );
        
        return ApiResponse.ok(indicators, "지원 보조지표 목록 조회 완료");
    }

    /**
     * 캐시 무효화
     * 
     * @param ticker 종목코드
     * @param interval 캔들 간격
     * @param indicatorType 지표 타입
     * @param params 지표 파라미터
     * @return 성공 메시지
     */
    @DeleteMapping("/cache/{ticker}/{indicatorType}")
    public ResponseEntity<?> invalidateCache(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1m") String interval,
            @PathVariable IndicatorType indicatorType,
            @RequestParam(required = false) Map<String, Object> params) {
        
        log.info("캐시 무효화 요청: ticker={}, interval={}, indicatorType={}", 
                ticker, interval, indicatorType);
        
        indicatorService.invalidateCache(ticker, interval, indicatorType, params);
        
        return ApiResponse.ok(null, "캐시 무효화 완료");
    }

    /**
     * 종목의 모든 캐시 무효화
     * 
     * @param ticker 종목코드
     * @return 성공 메시지
     */
    @DeleteMapping("/cache/{ticker}/all")
    public ResponseEntity<?> invalidateAllCache(@PathVariable String ticker) {
        log.info("종목 전체 캐시 무효화 요청: ticker={}", ticker);
        
        indicatorService.invalidateAllCacheForTicker(ticker);
        
        return ApiResponse.ok(null, "종목 전체 캐시 무효화 완료");
    }

    /**
     * 캐시 통계 조회
     * 
     * @param ticker 종목코드
     * @return 캐시 통계
     */
    @GetMapping("/cache/{ticker}/stats")
    public ResponseEntity<?> getCacheStats(@PathVariable String ticker) {
        log.info("캐시 통계 조회: ticker={}", ticker);
        
        Map<String, Object> stats = indicatorService.getCacheStats(ticker);
        
        return ApiResponse.ok(stats, "캐시 통계 조회 완료");
    }
}
