package com.beyond.MKX.domain.member.repository;

import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.member.entity.MemberStatus;
import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByEmail(String email);

    /** EMAIL_DUPLICATE_CHECK: 이메일 중복 여부 확인 */
    boolean existsByEmail(String email);

    Optional<Member> findByPhone(String phone);

    /** EMAIL_VERIFICATION: 이메일 인증 토큰으로 회원 조회 */
    Optional<Member> findByEmailVerificationToken(String token);

    /** PASSWORD_RESET: 비밀번호 재설정 토큰으로 회원 조회 */
    Optional<Member> findByPasswordResetToken(String token);

    List<Member> findByBrokerage(SecuritiesFirm brokerage);

    Optional<Member> findByIdAndBrokerage(UUID id, SecuritiesFirm brokerage);

    @Query(
            value = """
                select m
                from Member m
                where (:brokerageId is null or m.brokerage.id = :brokerageId)
                  and (:status is null or m.status = :status)
                  and (
                       :q is null
                    or lower(m.name) like lower(concat('%', :q, '%'))
                    or lower(m.email) like lower(concat('%', :q, '%'))
                  )
                """,
            countQuery = """
                select count(m)
                from Member m
                where (:brokerageId is null or m.brokerage.id = :brokerageId)
                  and (:status is null or m.status = :status)
                  and (
                       :q is null
                    or lower(m.name) like lower(concat('%', :q, '%'))
                    or lower(m.email) like lower(concat('%', :q, '%'))
                  )
                """
    )
    Page<Member> searchAdmin(
            @Param("brokerageId") UUID brokerageId,
            @Param("status") MemberStatus status,
            @Param("q") String q,
            Pageable pageable
    );
}
