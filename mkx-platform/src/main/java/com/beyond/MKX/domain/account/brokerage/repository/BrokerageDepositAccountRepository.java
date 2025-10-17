package com.beyond.MKX.domain.account.brokerage.repository;

import com.beyond.MKX.domain.account.brokerage.entity.BrokerageDepositAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 증권사 예치금 계좌 리포지토리
 */
@Repository
public interface BrokerageDepositAccountRepository extends JpaRepository<BrokerageDepositAccount, UUID> {
    Optional<BrokerageDepositAccount> findByBrokerageId(UUID brokerageId);

    Optional<BrokerageDepositAccount> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BrokerageDepositAccount a where a.accountNumber = :no")
    Optional<BrokerageDepositAccount> findByAccountNumberForUpdate(String no);
}
