package com.beyond.MKX.domain.execution.repository;

import com.beyond.MKX.domain.execution.entity.Ledger;
import com.beyond.MKX.domain.execution.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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
                                      @Param("startDateTime") LocalDateTime startDateTime,
                                      @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * 증권사별 수익 조회 (매수/매도 수수료) - 날짜 범위 기준
     * transactionType이 BUY 또는 SELL이고 creditAccountId 또는 debitAccountId가 brokerageId와 일치하는 ledger의 commission 합계
     * 
     * @param brokerageId 증권사 ID
     * @param transactionType 거래 유형
     * @param startDateTime 시작 일시
     * @param endDateTime 종료 일시
     * @return 매수 수수료 합계
     */
    @Query("SELECT COALESCE(SUM(l.commission), 0) FROM Ledger l " +
           "WHERE l.transactionType = :transactionType " +
           "AND (l.creditAccountId = :brokerageId OR l.debitAccountId = :brokerageId) " +
           "AND l.commission IS NOT NULL AND l.commission > 0 " +
           "AND l.createdAt >= :startDateTime AND l.createdAt < :endDateTime")
    Long getCommissionByBrokerageIdAndDateRange(@Param("brokerageId") UUID brokerageId,
                                                 @Param("transactionType") TransactionType transactionType,
                                                 @Param("startDateTime") LocalDateTime startDateTime,
                                                 @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * 증권사별 총 수수료 합산 조회 (모든 거래 유형)
     * 
     * @param brokerageId 증권사 ID
     * @param startDateTime 시작 일시
     * @param endDateTime 종료 일시
     * @return 총 수수료 합계
     */
    @Query("SELECT COALESCE(SUM(l.commission), 0) FROM Ledger l " +
           "WHERE (l.creditAccountId = :brokerageId OR l.debitAccountId = :brokerageId) " +
           "AND l.commission IS NOT NULL AND l.commission > 0 " +
           "AND l.createdAt >= :startDateTime AND l.createdAt < :endDateTime")
    Long getTotalCommissionByBrokerageIdAndDateRange(@Param("brokerageId") UUID brokerageId,
                                                      @Param("startDateTime") LocalDateTime startDateTime,
                                                      @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * 증권사별 거래 건수 조회
     * 
     * @param brokerageId 증권사 ID
     * @param startDateTime 시작 일시
     * @param endDateTime 종료 일시
     * @return 거래 건수
     */
    @Query("SELECT COUNT(l) FROM Ledger l " +
           "WHERE (l.creditAccountId = :brokerageId OR l.debitAccountId = :brokerageId) " +
           "AND l.transactionType IN ('BUY', 'SELL') " +
           "AND l.createdAt >= :startDateTime AND l.createdAt < :endDateTime")
    Long getTradeCountByBrokerageIdAndDateRange(@Param("brokerageId") UUID brokerageId,
                                                 @Param("startDateTime") LocalDateTime startDateTime,
                                                 @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * 증권사별 시간대별 수수료 집계
     * 
     * @param brokerageId 증권사 ID
     * @param startDateTime 시작 일시
     * @param endDateTime 종료 일시
     * @return [hour, totalCommission, buyCommission, sellCommission, tradeCount] 배열 리스트
     */
    @Query("SELECT HOUR(l.createdAt) as hour, " +
           "COALESCE(SUM(l.commission), 0) as totalCommission, " +
           "COALESCE(SUM(CASE WHEN l.transactionType = 'BUY' THEN l.commission ELSE 0 END), 0) as buyCommission, " +
           "COALESCE(SUM(CASE WHEN l.transactionType = 'SELL' THEN l.commission ELSE 0 END), 0) as sellCommission, " +
           "COUNT(l) as tradeCount " +
           "FROM Ledger l " +
           "WHERE (l.creditAccountId = :brokerageId OR l.debitAccountId = :brokerageId) " +
           "AND l.transactionType IN ('BUY', 'SELL') " +
           "AND l.commission IS NOT NULL AND l.commission > 0 " +
           "AND l.createdAt >= :startDateTime AND l.createdAt < :endDateTime " +
           "GROUP BY HOUR(l.createdAt) " +
           "ORDER BY hour")
    List<Object[]> getHourlyCommissionTrends(@Param("brokerageId") UUID brokerageId,
                                             @Param("startDateTime") LocalDateTime startDateTime,
                                             @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * 증권사별 일별 수수료 집계
     * 
     * @param brokerageId 증권사 ID
     * @param startDateTime 시작 일시
     * @param endDateTime 종료 일시
     * @return [date, totalCommission, buyCommission, sellCommission, tradeCount] 배열 리스트
     */
    @Query("SELECT FUNCTION('DATE', l.createdAt) as date, " +
           "COALESCE(SUM(l.commission), 0) as totalCommission, " +
           "COALESCE(SUM(CASE WHEN l.transactionType = 'BUY' THEN l.commission ELSE 0 END), 0) as buyCommission, " +
           "COALESCE(SUM(CASE WHEN l.transactionType = 'SELL' THEN l.commission ELSE 0 END), 0) as sellCommission, " +
           "COUNT(l) as tradeCount " +
           "FROM Ledger l " +
           "WHERE (l.creditAccountId = :brokerageId OR l.debitAccountId = :brokerageId) " +
           "AND l.transactionType IN ('BUY', 'SELL') " +
           "AND l.commission IS NOT NULL AND l.commission > 0 " +
           "AND l.createdAt >= :startDateTime AND l.createdAt < :endDateTime " +
           "GROUP BY FUNCTION('DATE', l.createdAt) " +
           "ORDER BY date")
    List<Object[]> getDailyCommissionBreakdown(@Param("brokerageId") UUID brokerageId,
                                               @Param("startDateTime") LocalDateTime startDateTime,
                                               @Param("endDateTime") LocalDateTime endDateTime);
}
