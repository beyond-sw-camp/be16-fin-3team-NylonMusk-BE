package com.beyond.MKX.common.auth.security;

import com.beyond.MKX.domain.member.entity.MemberStatus;

import java.util.UUID;

/**
 * 회원 인증 컨텍스트에 저장될 최소 정보.
 */
public record CustomMemberPrincipal(UUID id, MemberStatus status) {

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }
}
