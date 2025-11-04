package com.beyond.MKX.domain.brokerage.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.account.admin.client.AdminInternalClient;
import com.beyond.MKX.domain.brokerage.dto.BrokerageStatsDTO;
import com.beyond.MKX.domain.brokerage.dto.LedgerDTO;
import com.beyond.MKX.domain.brokerage.dto.OrderLogDTO;
import com.beyond.MKX.domain.brokerage.dto.PopularStockDTO;
import com.beyond.MKX.domain.brokerage.dto.RecentActivityDTO;
import com.beyond.MKX.domain.brokerage.service.BrokerageDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 증권사 대시보드 API
 * - 증권사 관리자용 대시보드 데이터 조회
 */
@RestController
@RequestMapping("/api/brokerage/dashboard")
@RequiredArgsConstructor
@Slf4j
public class BrokerageDashboardController {

    private final BrokerageDashboardService brokerageDashboardService;
    private final AdminInternalClient adminInternalClient;

    /**
     * 증권사별 통계 조회 (일일 거래량, 월간 수익 등)
     * 
     * @param adminId X-User-Id 헤더 (관리자 UUID)
     * @return 통계 데이터
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getBrokerageStats(
            @RequestHeader(value = "X-User-Id", required = false) String adminId
    ) {
        try {
            if (adminId == null || adminId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "X-User-Id 헤더가 필요합니다."));
            }
            UUID brokerageId = getBrokerageId(UUID.fromString(adminId));
            BrokerageStatsDTO stats = brokerageDashboardService.getBrokerageStats(brokerageId);
            return ApiResponse.ok(stats, "통계 조회 성공");
        } catch (Exception e) {
            log.error("Failed to get brokerage stats: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "통계 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 증권사별 최근 활동 목록 조회 (order_log + ledger 통합)
     * 
     * @param adminId X-User-Id 헤더 (관리자 UUID)
     * @param limit 최대 조회 개수 (기본값: 20)
     * @return 최근 활동 목록
     */
    @GetMapping("/recent-activities")
    public ResponseEntity<?> getRecentActivities(
            @RequestHeader(value = "X-User-Id", required = false) String adminId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        try {
            if (adminId == null || adminId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "X-User-Id 헤더가 필요합니다."));
            }
            UUID brokerageId = getBrokerageId(UUID.fromString(adminId));
            List<RecentActivityDTO> activities = brokerageDashboardService.getRecentActivities(brokerageId, limit);
            return ApiResponse.ok(activities, "최근 활동 목록 조회 성공");
        } catch (Exception e) {
            log.error("Failed to get recent activities for brokerage: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "최근 활동 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 증권사별 인기 종목 조회 (보유 고객 수 기준)
     * 
     * @param adminId X-User-Id 헤더 (관리자 UUID)
     * @param limit 최대 조회 개수 (기본값: 10)
     * @return 인기 종목 목록
     */
    @GetMapping("/popular-stocks")
    public ResponseEntity<?> getPopularStocks(
            @RequestHeader(value = "X-User-Id", required = false) String adminId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            if (adminId == null || adminId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "X-User-Id 헤더가 필요합니다."));
            }
            UUID brokerageId = getBrokerageId(UUID.fromString(adminId));
            List<PopularStockDTO> popularStocks = brokerageDashboardService.getPopularStocks(brokerageId, limit);
            return ApiResponse.ok(popularStocks, "인기 종목 조회 성공");
        } catch (Exception e) {
            log.error("Failed to get popular stocks for brokerage: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "인기 종목 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 증권사별 주문 내역 조회
     * 
     * @param adminId X-User-Id 헤더 (관리자 UUID)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 주문 내역 페이지
     */
    @GetMapping("/order-logs")
    public ResponseEntity<?> getOrderLogs(
            @RequestHeader(value = "X-User-Id", required = false) String adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            if (adminId == null || adminId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "X-User-Id 헤더가 필요합니다."));
            }
            UUID brokerageId = getBrokerageId(UUID.fromString(adminId));
            Pageable pageable = PageRequest.of(page, size);
            Page<OrderLogDTO> orderLogs = brokerageDashboardService.getOrderLogs(brokerageId, pageable);
            return ApiResponse.ok(orderLogs, "주문 내역 조회 성공");
        } catch (Exception e) {
            log.error("Failed to get order logs for brokerage: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "주문 내역 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 증권사별 거래내역 조회
     * 
     * @param adminId X-User-Id 헤더 (관리자 UUID)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 거래내역 페이지
     */
    @GetMapping("/ledgers")
    public ResponseEntity<?> getLedgers(
            @RequestHeader(value = "X-User-Id", required = false) String adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            if (adminId == null || adminId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "X-User-Id 헤더가 필요합니다."));
            }
            UUID brokerageId = getBrokerageId(UUID.fromString(adminId));
            Pageable pageable = PageRequest.of(page, size);
            Page<LedgerDTO> ledgers = brokerageDashboardService.getLedgers(brokerageId, pageable);
            return ApiResponse.ok(ledgers, "거래내역 조회 성공");
        } catch (Exception e) {
            log.error("Failed to get ledgers for brokerage: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "거래내역 조회 실패: " + e.getMessage()));
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

