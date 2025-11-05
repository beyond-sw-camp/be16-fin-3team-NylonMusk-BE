package com.beyond.MKX.domain.assets.repository;


import com.beyond.MKX.domain.assets.entity.MemberAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberAccountRepository extends JpaRepository<MemberAccount, UUID> {

    /** 계좌번호로 단건 조회 (UNIQUE INDEX 보유) */
    Optional<MemberAccount> findByNumber(String number);

    /** 특정 회원 + 증권사 계좌 존재 여부 확인 (복합 인덱스 사용) */
    boolean existsByMemberIdAndBrokerageId(UUID memberId, UUID brokerageId);

    /** 특정 회원 + 증권사 계좌 단건 조회 */
    Optional<MemberAccount> findByMemberIdAndBrokerageId(UUID memberId, UUID brokerageId);

    /** 증권사 소속 모든 회원 계좌 조회 (brokerage_id 인덱스 사용) */
    List<MemberAccount> findAllByBrokerageId(UUID brokerageId);

    /** 특정 회원의 가장 최근 생성 계좌 조회 (created_at 기준) */
    Optional<MemberAccount> findFirstByMemberIdOrderByCreatedAtDesc(UUID memberId);

    /** 특정 회원의 계좌 조회  */
    Optional<MemberAccount> findByMemberId(UUID memberId);

    /**
     * ID로 계좌 조회 (비관적 잠금 사용)
     * 동시 주문 처리 시 DB 충돌 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ma FROM MemberAccount ma WHERE ma.id = :accountId")
    Optional<MemberAccount> findByIdWithLock(@Param("accountId") UUID accountId);

}
