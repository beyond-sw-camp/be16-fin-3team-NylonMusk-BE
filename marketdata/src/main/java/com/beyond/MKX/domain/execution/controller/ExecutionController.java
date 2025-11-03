package com.beyond.MKX.domain.execution.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.execution.dto.ExecutionEventDTO;
import com.beyond.MKX.domain.execution.dto.PagedExecutionResponse;
import com.beyond.MKX.domain.execution.entity.Execution;
import com.beyond.MKX.domain.execution.repository.ExecutionInfluxRepository;
import com.beyond.MKX.domain.execution.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 체결 데이터 REST API Controller
 * 
 * 체결 데이터 조회 및 페이징 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;
    private final ExecutionInfluxRepository executionRepository;

    /**
     * 페이징된 체결 데이터 조회
     * 
     * GET /api/v1/executions?ticker=AAPL&start=2024-11-01T00:00:00&end=2024-11-03T23:59:59&page=0&size=20
     * 
     * @param ticker 종목 코드 (필수)
     * @param start 시작 시각 (선택, 기본값: 24시간 전)
     * @param end 종료 시각 (선택, 기본값: 현재)
     * @param page 페이지 번호 (선택, 기본값: 0)
     * @param size 페이지 크기 (선택, 기본값: 20, 최대: 1000)
     * @return 페이징된 체결 데이터
     */
    @GetMapping
    public ResponseEntity<?> getExecutions(
            @RequestParam String ticker,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            log.info("[EXECUTION-API] GET /api/v1/executions - ticker={}, start={}, end={}, page={}, size={}", 
                    ticker, start, end, page, size);
            
            // 파라미터 검증 및 기본값 설정
            if (size > 1000) {
                size = 1000; // 최대 1000개로 제한
            }
            if (size < 1) {
                size = 20;
            }
            if (page < 0) {
                page = 0;
            }
            
            // 시작/종료 시각 기본값 설정
            Instant endInstant = end != null 
                    ? end.atZone(ZoneId.systemDefault()).toInstant() 
                    : Instant.now();
            Instant startInstant = start != null 
                    ? start.atZone(ZoneId.systemDefault()).toInstant() 
                    : endInstant.minus(24, java.time.temporal.ChronoUnit.HOURS);
            
            // 페이징된 데이터 조회
            PagedExecutionResponse response = executionService.getExecutionsWithPaging(
                    ticker, startInstant, endInstant, page, size);
            
            log.info("[EXECUTION-API] ✅ Retrieved {} executions (page {}/{})", 
                    response.getContent().size(), page, response.getTotalPages());
            
            return ApiResponse.ok(response);
            
        } catch (Exception e) {
            log.error("[EXECUTION-API] ❌ Failed to get executions", e);
            return ApiResponse.noContent("Failed to retrieve executions: " + e.getMessage());
        }
    }

    /**
     * 최근 체결 데이터 조회 (간편 API)
     * 
     * GET /api/v1/executions/recent?ticker=AAPL&limit=100
     * 
     * @param ticker 종목 코드 (필수)
     * @param limit 조회할 개수 (선택, 기본값: 50, 최대: 1000)
     * @return 최근 체결 데이터 리스트
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentExecutions(
            @RequestParam String ticker,
            @RequestParam(defaultValue = "50") int limit) {
        
        try {
            log.info("[EXECUTION-API] GET /api/v1/executions/recent - ticker={}, limit={}", ticker, limit);
            
            // limit 검증
            if (limit > 1000) {
                limit = 1000;
            }
            if (limit < 1) {
                limit = 50;
            }
            
            // 최근 데이터 조회
            List<ExecutionEventDTO> executions = executionService.getRecentExecutions(ticker, limit);
            
            log.info("[EXECUTION-API] ✅ Retrieved {} recent executions", executions.size());
            
            return ApiResponse.ok(executions);
            
        } catch (Exception e) {
            log.error("[EXECUTION-API] ❌ Failed to get recent executions", e);
            return ApiResponse.noContent("Failed to retrieve recent executions: " + e.getMessage());
        }
    }

    /**
     * 특정 기간의 전체 체결 데이터 조회 (페이징 없음, 대용량 주의)
     * 
     * GET /api/v1/executions/all?ticker=AAPL&start=2024-11-01T00:00:00&end=2024-11-01T23:59:59
     * 
     * @param ticker 종목 코드 (필수)
     * @param start 시작 시각 (필수)
     * @param end 종료 시각 (필수)
     * @return 전체 체결 데이터 리스트
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllExecutions(
            @RequestParam String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        try {
            log.info("[EXECUTION-API] GET /api/v1/executions/all - ticker={}, start={}, end={}", 
                    ticker, start, end);
            
            Instant startInstant = start.atZone(ZoneId.systemDefault()).toInstant();
            Instant endInstant = end.atZone(ZoneId.systemDefault()).toInstant();
            
            // 전체 데이터 조회
            List<Execution> executions = executionRepository.findExecutions(ticker, startInstant, endInstant);
            
            log.info("[EXECUTION-API] ✅ Retrieved {} total executions", executions.size());
            
            return ApiResponse.ok(executions);
            
        } catch (Exception e) {
            log.error("[EXECUTION-API] ❌ Failed to get all executions", e);
            return ApiResponse.noContent("Failed to retrieve all executions: " + e.getMessage());
        }
    }

    /**
     * OHLCV 데이터 조회 (차트용)
     * 
     * GET /api/v1/executions/ohlcv?ticker=AAPL&interval=1m&duration=24h
     * 
     * @param ticker 종목 코드 (필수)
     * @param interval 간격 (선택, 기본값: 1m, 예: 1m, 5m, 15m, 1h, 1d)
     * @param duration 기간 (선택, 기본값: 24h, 예: 1h, 24h, 7d)
     * @return OHLCV 데이터 리스트
     */
    @GetMapping("/ohlcv")
    public ResponseEntity<?> getOHLCVData(
            @RequestParam String ticker,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam(defaultValue = "24h") String duration) {
        
        try {
            log.info("[EXECUTION-API] GET /api/v1/executions/ohlcv - ticker={}, interval={}, duration={}", 
                    ticker, interval, duration);
            
            List<ExecutionInfluxRepository.OHLCVData> ohlcv = 
                    executionRepository.getOHLCVData(ticker, interval, duration);
            
            log.info("[EXECUTION-API] ✅ Retrieved {} OHLCV data points", ohlcv.size());
            
            return ApiResponse.ok(ohlcv);
            
        } catch (Exception e) {
            log.error("[EXECUTION-API] ❌ Failed to get OHLCV data", e);
            return ApiResponse.noContent("Failed to retrieve OHLCV data: " + e.getMessage());
        }
    }

    /**
     * 체결 데이터 개수 조회
     * 
     * GET /api/v1/executions/count?ticker=AAPL&start=2024-11-01T00:00:00&end=2024-11-03T23:59:59
     * 
     * @param ticker 종목 코드 (필수)
     * @param start 시작 시각 (선택, 기본값: 24시간 전)
     * @param end 종료 시각 (선택, 기본값: 현재)
     * @return 체결 데이터 개수
     */
    @GetMapping("/count")
    public ResponseEntity<?> countExecutions(
            @RequestParam String ticker,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        try {
            log.info("[EXECUTION-API] GET /api/v1/executions/count - ticker={}, start={}, end={}", 
                    ticker, start, end);
            
            Instant endInstant = end != null 
                    ? end.atZone(ZoneId.systemDefault()).toInstant() 
                    : Instant.now();
            Instant startInstant = start != null 
                    ? start.atZone(ZoneId.systemDefault()).toInstant() 
                    : endInstant.minus(24, java.time.temporal.ChronoUnit.HOURS);
            
            long count = executionRepository.countExecutions(ticker, startInstant, endInstant);
            
            log.info("[EXECUTION-API] ✅ Count: {}", count);
            
            return ApiResponse.ok(count);
            
        } catch (Exception e) {
            log.error("[EXECUTION-API] ❌ Failed to count executions", e);
            return ApiResponse.noContent("Failed to count executions: " + e.getMessage());
        }
    }
}
