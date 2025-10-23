package com.beyond.MKX.domain.delisting.controller;

import com.beyond.MKX.domain.delisting.entity.ExchangeSupportFund;
import com.beyond.MKX.domain.delisting.repository.ExchangeSupportFundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 거래소 지원금 관리 Controller
 * 
 * 거래소에서 제공한 지원금(대출) 정보를 관리합니다.
 * 
 * @author MKX Platform Team
 * @since 2025-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/delisting/support-fund")
@RequiredArgsConstructor
public class ExchangeSupportFundController {

    private final ExchangeSupportFundRepository supportFundRepo;

    /**
     * 모든 지원금 조회
     */
    @GetMapping
    public ResponseEntity<List<ExchangeSupportFund>> getAllSupportFunds() {
        List<ExchangeSupportFund> supportFunds = supportFundRepo.findAll();
        return ResponseEntity.ok(supportFunds);
    }

    /**
     * 주식별 지원금 조회
     */
    @GetMapping("/stock/{stockId}")
    public ResponseEntity<List<ExchangeSupportFund>> getSupportFundsByStock(@PathVariable UUID stockId) {
        List<ExchangeSupportFund> supportFunds = supportFundRepo.findByStockId(stockId);
        return ResponseEntity.ok(supportFunds);
    }

    /**
     * 기업별 지원금 조회
     */
    @GetMapping("/corporation/{corporationId}")
    public ResponseEntity<List<ExchangeSupportFund>> getSupportFundsByCorporation(@PathVariable UUID corporationId) {
        List<ExchangeSupportFund> supportFunds = supportFundRepo.findByCorporationId(corporationId);
        return ResponseEntity.ok(supportFunds);
    }

    /**
     * 상태별 지원금 조회
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ExchangeSupportFund>> getSupportFundsByStatus(@PathVariable ExchangeSupportFund.SupportStatus status) {
        List<ExchangeSupportFund> supportFunds = supportFundRepo.findByStatus(status);
        return ResponseEntity.ok(supportFunds);
    }

    /**
     * 연체된 지원금 조회
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<ExchangeSupportFund>> getOverdueSupportFunds() {
        List<ExchangeSupportFund> overdueFunds = supportFundRepo.findOverdueSupports();
        return ResponseEntity.ok(overdueFunds);
    }

    /**
     * 기업별 총 지원금 합계 조회
     */
    @GetMapping("/corporation/{corporationId}/total")
    public ResponseEntity<BigDecimal> getTotalSupportAmountByCorporation(@PathVariable UUID corporationId) {
        BigDecimal totalAmount = supportFundRepo.getTotalSupportAmountByCorporation(corporationId);
        return ResponseEntity.ok(totalAmount != null ? totalAmount : BigDecimal.ZERO);
    }

    /**
     * 기업별 미상환 금액 조회
     */
    @GetMapping("/corporation/{corporationId}/unpaid")
    public ResponseEntity<BigDecimal> getUnpaidAmountByCorporation(@PathVariable UUID corporationId) {
        BigDecimal unpaidAmount = supportFundRepo.getUnpaidAmountByCorporation(corporationId);
        return ResponseEntity.ok(unpaidAmount != null ? unpaidAmount : BigDecimal.ZERO);
    }

    /**
     * 거래소 총 지원금 합계 조회
     */
    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getTotalActiveSupportAmount() {
        BigDecimal totalAmount = supportFundRepo.getTotalActiveSupportAmount();
        return ResponseEntity.ok(totalAmount != null ? totalAmount : BigDecimal.ZERO);
    }

    /**
     * 거래소 총 미상환 금액 조회
     */
    @GetMapping("/total-unpaid")
    public ResponseEntity<BigDecimal> getTotalUnpaidAmount() {
        BigDecimal unpaidAmount = supportFundRepo.getTotalUnpaidAmount();
        return ResponseEntity.ok(unpaidAmount != null ? unpaidAmount : BigDecimal.ZERO);
    }

    /**
     * 지원금 상환 처리
     */
    @PostMapping("/{supportFundId}/repay")
    public ResponseEntity<String> repaySupportFund(@PathVariable UUID supportFundId, @RequestParam BigDecimal amount) {
        try {
            ExchangeSupportFund supportFund = supportFundRepo.findById(supportFundId)
                    .orElseThrow(() -> new IllegalArgumentException("지원금을 찾을 수 없습니다: " + supportFundId));
            
            supportFund.repay(amount);
            supportFundRepo.save(supportFund);
            
            log.info("지원금 상환 처리 완료: supportFundId={}, amount={}, remaining={}", 
                    supportFundId, amount, supportFund.getRemainingAmount());
            
            return ResponseEntity.ok("상환 처리 완료. 잔여 금액: " + supportFund.getRemainingAmount());
            
        } catch (Exception e) {
            log.error("지원금 상환 처리 실패: supportFundId={}, amount={}", supportFundId, amount, e);
            return ResponseEntity.badRequest().body("상환 처리 실패: " + e.getMessage());
        }
    }

    /**
     * 지원금 연체 처리
     */
    @PostMapping("/{supportFundId}/mark-overdue")
    public ResponseEntity<String> markOverdue(@PathVariable UUID supportFundId) {
        try {
            ExchangeSupportFund supportFund = supportFundRepo.findById(supportFundId)
                    .orElseThrow(() -> new IllegalArgumentException("지원금을 찾을 수 없습니다: " + supportFundId));
            
            supportFund.markOverdue();
            supportFundRepo.save(supportFund);
            
            log.info("지원금 연체 처리 완료: supportFundId={}", supportFundId);
            
            return ResponseEntity.ok("연체 처리 완료");
            
        } catch (Exception e) {
            log.error("지원금 연체 처리 실패: supportFundId={}", supportFundId, e);
            return ResponseEntity.badRequest().body("연체 처리 실패: " + e.getMessage());
        }
    }

    /**
     * 지원금 대손 처리
     */
    @PostMapping("/{supportFundId}/write-off")
    public ResponseEntity<String> writeOffSupportFund(@PathVariable UUID supportFundId) {
        try {
            ExchangeSupportFund supportFund = supportFundRepo.findById(supportFundId)
                    .orElseThrow(() -> new IllegalArgumentException("지원금을 찾을 수 없습니다: " + supportFundId));
            
            supportFund.writeOff();
            supportFundRepo.save(supportFund);
            
            log.info("지원금 대손 처리 완료: supportFundId={}", supportFundId);
            
            return ResponseEntity.ok("대손 처리 완료");
            
        } catch (Exception e) {
            log.error("지원금 대손 처리 실패: supportFundId={}", supportFundId, e);
            return ResponseEntity.badRequest().body("대손 처리 실패: " + e.getMessage());
        }
    }
}
