package com.beyond.MKX.domain.delisting.repository;

import com.beyond.MKX.domain.delisting.entity.ExchangeSupportFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 거래소 지원금 Repository
 * 
 * 거래소에서 제공한 지원금(대출) 정보를 관리합니다.
 * 
 * @author MKX Platform Team
 * @since 2025-01-15
 */
@Repository
public interface ExchangeSupportFundRepository extends JpaRepository<ExchangeSupportFund, UUID> {

    /**
     * 주식 ID로 지원금 조회
     */
    List<ExchangeSupportFund> findByStockId(UUID stockId);

    /**
     * 기업 ID로 지원금 조회
     */
    List<ExchangeSupportFund> findByCorporationId(UUID corporationId);

    /**
     * 상태별 지원금 조회
     */
    List<ExchangeSupportFund> findByStatus(ExchangeSupportFund.SupportStatus status);

    /**
     * 활성 상태인 지원금 조회
     */
    List<ExchangeSupportFund> findByStatusIn(List<ExchangeSupportFund.SupportStatus> statuses);

    /**
     * 연체된 지원금 조회
     */
    @Query("SELECT e FROM ExchangeSupportFund e WHERE e.status = 'OVERDUE' AND e.repaymentDueDate < CURRENT_TIMESTAMP")
    List<ExchangeSupportFund> findOverdueSupports();

    /**
     * 기업별 총 지원금 합계 조회 (상환 완료된 것도 포함)
     */
    @Query("SELECT SUM(e.supportAmount) FROM ExchangeSupportFund e WHERE e.corporationId = :corporationId AND e.status IN ('ACTIVE', 'PARTIAL_REPAID', 'OVERDUE', 'REPAID')")
    BigDecimal getTotalSupportAmountByCorporation(@Param("corporationId") UUID corporationId);

    /**
     * 기업별 미상환 금액 조회 (상환 완료된 것은 0으로 계산됨)
     */
    @Query("SELECT SUM(e.supportAmount - e.repaidAmount) FROM ExchangeSupportFund e WHERE e.corporationId = :corporationId AND e.status IN ('ACTIVE', 'PARTIAL_REPAID', 'OVERDUE', 'REPAID')")
    BigDecimal getUnpaidAmountByCorporation(@Param("corporationId") UUID corporationId);

    /**
     * 거래소 총 지원금 합계 조회 (상환 완료된 것도 포함)
     */
    @Query("SELECT SUM(e.supportAmount) FROM ExchangeSupportFund e WHERE e.status IN ('ACTIVE', 'PARTIAL_REPAID', 'OVERDUE', 'REPAID')")
    BigDecimal getTotalActiveSupportAmount();

    /**
     * 거래소 총 미상환 금액 조회 (상환 완료된 것은 0으로 계산됨)
     */
    @Query("SELECT SUM(e.supportAmount - e.repaidAmount) FROM ExchangeSupportFund e WHERE e.status IN ('ACTIVE', 'PARTIAL_REPAID', 'OVERDUE', 'REPAID')")
    BigDecimal getTotalUnpaidAmount();
}
