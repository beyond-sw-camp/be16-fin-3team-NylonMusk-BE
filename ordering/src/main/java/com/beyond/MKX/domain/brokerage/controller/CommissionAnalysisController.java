package com.beyond.MKX.domain.brokerage.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.brokerage.dto.DailyCommissionDTO;
import com.beyond.MKX.domain.brokerage.dto.HourlyCommissionTrendDTO;
import com.beyond.MKX.domain.brokerage.dto.PeriodCommissionDTO;
import com.beyond.MKX.domain.brokerage.service.CommissionAnalysisService;
import com.beyond.MKX.domain.account.admin.client.AdminInternalClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 증권사 수수료 분석 API
 * - 일일 수수료, 기간별 수수료, 시간대별 수익 추이 조회
 */
@RestController
@RequestMapping("/api/brokerage/commission")
@RequiredArgsConstructor
@Slf4j
public class CommissionAnalysisController {

    private final CommissionAnalysisService commissionAnalysisService;
    private final AdminInternalClient adminInternalClient;

    /**
     * 일일 수수료 합산 조회 (당일 거래내역에서 합산)
     * GET /api/brokerage/commission/daily
     * 
     * @param adminId X-User-Id 헤더 (관리자 UUID)
     * @param date 조회 날짜 (기본값: 오늘, 형식: yyyy-MM-dd)
     * @return 일일 수수료 데이터
     */
    @GetMapping("/daily")
    public ResponseEntity<?> getDailyCommission(
            @RequestHeader(value = "X-User-Id", required = false) String adminId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            if (adminId == null || adminId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "X-User-Id 헤더가 필요합니다."));
            }
            UUID brokerageId = getBrokerageId(UUID.fromString(adminId));
            DailyCommissionDTO result = commissionAnalysisService.getDailyCommission(brokerageId, date);
            return ApiResponse.ok(result, "일일 수수료 조회 성공");
        } catch (Exception e) {
            log.error("Failed to get daily commission: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "일일 수수료 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 기간별 수수료 합산 조회 (7일, 30일, 90일, 365일)
     * GET /api/brokerage/commission/period
     * 
     * @param adminId X-User-Id 헤더 (관리자 UUID)
     * @param days 조회 기간 (7, 30, 90, 365) (기본값: 7)
     * @return 기간별 수수료 데이터
     */
    @GetMapping("/period")
    public ResponseEntity<?> getCommissionByPeriod(
            @RequestHeader(value = "X-User-Id", required = false) String adminId,
            @RequestParam(required = false, defaultValue = "7") Integer days
    ) {
        try {
            if (adminId == null || adminId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "X-User-Id 헤더가 필요합니다."));
            }
            
            // days 유효성 검사
            if (days != 7 && days != 30 && days != 90 && days != 365) {
                return ResponseEntity.badRequest().body(
                    Map.of("message", "days는 7, 30, 90, 365 중 하나여야 합니다.")
                );
            }
            
            UUID brokerageId = getBrokerageId(UUID.fromString(adminId));
            PeriodCommissionDTO result = commissionAnalysisService.getCommissionByPeriod(brokerageId, days);
            return ApiResponse.ok(result, "기간별 수수료 조회 성공");
        } catch (Exception e) {
            log.error("Failed to get commission by period: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "기간별 수수료 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 시간대별 수익 추이 조회
     * GET /api/brokerage/commission/hourly-trends
     * 
     * @param adminId X-User-Id 헤더 (관리자 UUID)
     * @param days 조회 기간 (기본값: 7일)
     * @return 시간대별 수수료 추이 리스트
     */
    @GetMapping("/hourly-trends")
    public ResponseEntity<?> getHourlyCommissionTrends(
            @RequestHeader(value = "X-User-Id", required = false) String adminId,
            @RequestParam(required = false, defaultValue = "7") Integer days
    ) {
        try {
            if (adminId == null || adminId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "X-User-Id 헤더가 필요합니다."));
            }
            UUID brokerageId = getBrokerageId(UUID.fromString(adminId));
            List<HourlyCommissionTrendDTO> trends = commissionAnalysisService.getHourlyCommissionTrends(brokerageId, days);
            return ApiResponse.ok(trends, "시간대별 수익 추이 조회 성공");
        } catch (Exception e) {
            log.error("Failed to get hourly commission trends: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "시간대별 수익 추이 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 관리자 ID로 증권사 ID 조회
     */
    private UUID getBrokerageId(UUID adminId) {
        try {
            AdminInternalClient.BrokerageIdRes res = adminInternalClient.getBrokerageId(adminId);
            if (res == null || res.brokerageId() == null) {
                throw new IllegalArgumentException("증권사 소속 관리자가 아닙니다.");
            }
            return res.brokerageId();
        } catch (Exception e) {
            log.error("Failed to get brokerage ID for admin {}: {}", adminId, e.getMessage());
            throw new IllegalArgumentException("증권사 ID 조회 실패: " + e.getMessage());
        }
    }
}
