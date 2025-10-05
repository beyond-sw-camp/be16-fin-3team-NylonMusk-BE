package com.beyond.MKX.common.auth.security;

import com.beyond.MKX.domain.member.entity.MemberStatus;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 회원 권한을 메서드 단에서 점검하기 위한 가드.
 * - 현재 시점에도 ACTIVE 상태인지 확인하기 위해 DB를 한 번 더 조회한다.
 */
@Component("memberAccessGuard")
@RequiredArgsConstructor
public class MemberAccessGuard {

    private final MemberRepository memberRepository;

    public boolean isActiveMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .map(member -> member.getStatus() == MemberStatus.ACTIVE)
                .orElse(false);
    }
}
