package com.beyond.MKX.domain.account.corporation.repository;

import com.beyond.MKX.domain.account.corporation.entity.CorporationAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** 기업 계좌 리포지토리 */
@Repository
public interface CorporationAccountRepository extends JpaRepository<CorporationAccount, UUID> {
    Optional<CorporationAccount> findByAccountNumber(String accountNumber);
    // 하나의 기업당 계좌 1개 보유 가정 → 단건 조회 메서드 사용
    Optional<CorporationAccount> findByCorporationId(UUID corporationId);

}
