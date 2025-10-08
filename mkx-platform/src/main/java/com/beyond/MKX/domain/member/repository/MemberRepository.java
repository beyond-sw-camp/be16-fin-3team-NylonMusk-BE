package com.beyond.MKX.domain.member.repository;

import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByEmail(String email);

    Optional<Member> findByPhone(String phone);

    List<Member> findByBrokerage(SecuritiesFirm brokerage);

    Optional<Member> findByIdAndBrokerage(UUID id, SecuritiesFirm brokerage);
}
