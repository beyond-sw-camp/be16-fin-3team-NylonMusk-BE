package com.beyond.MKX.domain.account.exchange.repository;

import com.beyond.MKX.domain.account.exchange.entity.ExchangeAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExchangeAccountRepository extends JpaRepository<ExchangeAccount, UUID> {

    Optional<ExchangeAccount> findByAccountNumber(String accountNumber);
    // 운영 계좌는 단일이라고 가정 → 가장 이른 생성 1건 조회
    Optional<ExchangeAccount> findFirstByOrderByCreatedAtAsc();

    List<ExchangeAccount> findAll();
}