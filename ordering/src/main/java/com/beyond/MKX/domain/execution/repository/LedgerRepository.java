package com.beyond.MKX.domain.execution.repository;

import com.beyond.MKX.domain.execution.entity.Ledger;
import com.beyond.MKX.domain.execution.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
