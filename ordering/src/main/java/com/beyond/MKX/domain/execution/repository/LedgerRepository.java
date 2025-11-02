package com.beyond.MKX.domain.execution.repository;

import com.beyond.MKX.domain.execution.entity.Ledger;
import com.beyond.MKX.domain.execution.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LedgerRepository extends JpaRepository<Ledger, UUID> {
    
    /**
     * 특정 계좌의 거래내역을 조회합니다.
     * creditAccountId 또는 debitAccountId가 memberAccountId와 일치하는 레코드를 조회합니다.
     */
    Page<Ledger> findByCreditAccountIdOrDebitAccountIdOrderByCreatedAtDesc(
        UUID creditAccountId, UUID debitAccountId, Pageable pageable);
    
    /**
     * 특정 계좌의 거래내역을 거래 유형으로 필터링하여 조회합니다.
     */
    Page<Ledger> findByCreditAccountIdOrDebitAccountIdAndTransactionTypeOrderByCreatedAtDesc(
        UUID creditAccountId, UUID debitAccountId, TransactionType transactionType, Pageable pageable);
    
    /**
     * 증권사별 최근 거래내역 조회
     * creditAccountId 또는 debitAccountId가 증권사 ID와 일치하는 레코드를 조회합니다.
     */
    @Query("SELECT l FROM Ledger l WHERE l.creditAccountId = :brokerageId OR l.debitAccountId = :brokerageId ORDER BY l.createdAt DESC")
    Page<Ledger> findByBrokerageIdOrderByCreatedAtDesc(@Param("brokerageId") UUID brokerageId, Pageable pageable);

    /**
     * 증권사별 일일 거래량 조회 (특정 날짜 범위)
     * credit 또는 debit 중 큰 값을 합산하여 거래량 계산
     */
    @Query("SELECT COALESCE(SUM(GREATEST(COALESCE(l.credit, 0), COALESCE(l.debit, 0))), 0) FROM Ledger l " +
           "WHERE (l.creditAccountId = :brokerageId OR l.debitAccountId = :brokerageId) " +
           "AND l.createdAt >= :startDateTime AND l.createdAt < :endDateTime")
    Long getDailyVolumeByBrokerageId(@Param("brokerageId") UUID brokerageId, 
                                      @Param("startDateTime") java.time.LocalDateTime startDateTime,
                                      @Param("endDateTime") java.time.LocalDateTime endDateTime);

    /**
     * 증권사별 월간 수익 조회 (매수 수수료)
     * transactionType이 BUY이고 creditAccountId 또는 debitAccountId가 brokerageId와 일치하는 ledger의 commission 합계
     * 
     * @param brokerageId 증권사 ID
     * @param year 년도
     * @param month 월
     * @return 매수 수수료 합계
     */
    @Query("SELECT COALESCE(SUM(l.commission), 0) FROM Ledger l " +
           "WHERE l.transactionType = :transactionType " +
           "AND (l.creditAccountId = :brokerageId OR l.debitAccountId = :brokerageId) " +
           "AND l.commission IS NOT NULL AND l.commission > 0 " +
           "AND YEAR(l.createdAt) = :year AND MONTH(l.createdAt) = :month")
    Long getBuyCommissionByBrokerageId(@Param("brokerageId") UUID brokerageId,
                                        @Param("transactionType") TransactionType transactionType,
                                        @Param("year") int year,
                                        @Param("month") int month);

    /**
     * 증권사별 월간 수익 조회 (매도 수수료)
     * transactionType이 SELL이고 creditAccountId 또는 debitAccountId가 brokerageId와 일치하는 ledger의 commission 합계
     * 
     * @param brokerageId 증권사 ID
     * @param year 년도
     * @param month 월
     * @return 매도 수수료 합계
     */
    @Query("SELECT COALESCE(SUM(l.commission), 0) FROM Ledger l " +
           "WHERE l.transactionType = :transactionType " +
           "AND (l.creditAccountId = :brokerageId OR l.debitAccountId = :brokerageId) " +
           "AND l.commission IS NOT NULL AND l.commission > 0 " +
           "AND YEAR(l.createdAt) = :year AND MONTH(l.createdAt) = :month")
    Long getSellCommissionByBrokerageId(@Param("brokerageId") UUID brokerageId,
                                         @Param("transactionType") TransactionType transactionType,
                                         @Param("year") int year,
                                         @Param("month") int month);
}
