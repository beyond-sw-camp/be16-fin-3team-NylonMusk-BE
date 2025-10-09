package com.beyond.MKX.domain.account.member.repository;

import com.beyond.MKX.domain.account.member.entity.MemberAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 유저 계좌 Repository */
@Repository
public interface MemberAccountRepository extends JpaRepository<MemberAccount, UUID> {
    Optional<MemberAccount> findByAccountNumber(String accountNumber);
    boolean existsByMemberIdAndBrokerageId(UUID memberId, UUID brokerageId);
    Optional<MemberAccount> findByMemberIdAndBrokerageId(UUID memberId, UUID brokerageId);
    List<MemberAccount> findAllByBrokerageId(UUID brokerageId);
    Optional<MemberAccount> findFirstByMemberIdOrderByCreatedAtDesc(UUID memberId);
}
