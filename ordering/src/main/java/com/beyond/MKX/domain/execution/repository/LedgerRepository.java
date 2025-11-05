package com.beyond.MKX.domain.execution.repository;

import com.beyond.MKX.domain.execution.entity.Ledger;
import com.beyond.MKX.domain.execution.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface LedgerRepository extends JpaRepository<Ledger, UUID> {

    /**
     * 특정 계좌의 거래내역을 조회합니다.
     * (OR 조건 → UNION ALL로 분리하여 인덱스 병목 제거)
     */
    @Query(
            value = """
        (
            SELECT * FROM ledger
            WHERE credit_account_id = :creditAccountId
            ORDER BY created_at DESC
        )
        UNION ALL
        (
            SELECT * FROM ledger
            WHERE debit_account_id = :debitAccountId
            ORDER BY created_at DESC
        )
        ORDER BY created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM (
            SELECT id FROM ledger WHERE credit_account_id = :creditAccountId
            UNION ALL
            SELECT id FROM ledger WHERE debit_account_id = :debitAccountId
        ) AS count_query
        """,
            nativeQuery = true
    )
    Page<Ledger> findByCreditAccountIdOrDebitAccountIdOrderByCreatedAtDesc(
            @Param("creditAccountId") UUID creditAccountId,
            @Param("debitAccountId") UUID debitAccountId,
            Pageable pageable
    );

    /**
     * 특정 계좌의 거래내역을 거래 유형으로 필터링하여 조회합니다.
     */
    @Query(
            value = """
        (
            SELECT * FROM ledger
            WHERE credit_account_id = :creditAccountId
              AND transaction_type = :transactionType
            ORDER BY created_at DESC
        )
        UNION ALL
        (
            SELECT * FROM ledger
            WHERE debit_account_id = :debitAccountId
              AND transaction_type = :transactionType
            ORDER BY created_at DESC
        )
        ORDER BY created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM (
            SELECT id FROM ledger WHERE credit_account_id = :creditAccountId AND transaction_type = :transactionType
            UNION ALL
            SELECT id FROM ledger WHERE debit_account_id = :debitAccountId AND transaction_type = :transactionType
        ) AS count_query
        """,
            nativeQuery = true
    )
    Page<Ledger> findByCreditAccountIdOrDebitAccountIdAndTransactionTypeOrderByCreatedAtDesc(
            @Param("creditAccountId") UUID creditAccountId,
            @Param("debitAccountId") UUID debitAccountId,
            @Param("transactionType") TransactionType transactionType,
            Pageable pageable
    );

    /**
     * 증권사별 최근 거래내역 조회
     */
    @Query(
            value = """
        (
            SELECT * FROM ledger
            WHERE credit_account_id = :brokerageId
            ORDER BY created_at DESC
        )
        UNION ALL
        (
            SELECT * FROM ledger
            WHERE debit_account_id = :brokerageId
            ORDER BY created_at DESC
        )
        ORDER BY created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM (
            SELECT id FROM ledger WHERE credit_account_id = :brokerageId
            UNION ALL
            SELECT id FROM ledger WHERE debit_account_id = :brokerageId
        ) AS count_query
        """,
            nativeQuery = true
    )
    Page<Ledger> findByBrokerageIdOrderByCreatedAtDesc(
            @Param("brokerageId") UUID brokerageId,
            Pageable pageable
    );

    /**
     * 증권사별 일일 거래량 조회 (특정 날짜 범위)
     */
    @Query(
            value = """
        SELECT COALESCE(SUM(volume), 0) FROM (
            SELECT GREATEST(COALESCE(l.credit, 0), COALESCE(l.debit, 0)) AS volume
            FROM ledger l
            WHERE l.credit_account_id = :brokerageId
              AND l.created_at >= :startDateTime
              AND l.created_at < :endDateTime
            UNION ALL
            SELECT GREATEST(COALESCE(l.credit, 0), COALESCE(l.debit, 0)) AS volume
            FROM ledger l
            WHERE l.debit_account_id = :brokerageId
              AND l.created_at >= :startDateTime
              AND l.created_at < :endDateTime
        ) AS combined
        """,
            nativeQuery = true
    )
    Long getDailyVolumeByBrokerageId(
            @Param("brokerageId") UUID brokerageId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    /**
     * 증권사별 수익 조회 (매수 수수료)
     */
    @Query(
            value = """
        SELECT COALESCE(SUM(commission), 0) FROM (
            SELECT l.commission
            FROM ledger l
            WHERE l.transaction_type = :transactionType
              AND l.credit_account_id = :brokerageId
              AND l.commission IS NOT NULL AND l.commission > 0
              AND l.created_at >= :startDateTime AND l.created_at < :endDateTime
            UNION ALL
            SELECT l.commission
            FROM ledger l
            WHERE l.transaction_type = :transactionType
              AND l.debit_account_id = :brokerageId
              AND l.commission IS NOT NULL AND l.commission > 0
              AND l.created_at >= :startDateTime AND l.created_at < :endDateTime
        ) AS combined
        """,
            nativeQuery = true
    )
    Long getCommissionByBrokerageIdAndDateRange(
            @Param("brokerageId") UUID brokerageId,
            @Param("transactionType") TransactionType transactionType,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
