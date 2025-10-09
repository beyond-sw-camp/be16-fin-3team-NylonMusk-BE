package com.beyond.MKX.common.auth.security;

import com.beyond.MKX.domain.admin.entity.Role;
import com.beyond.MKX.domain.admin.entity.Status;

import java.util.UUID;

/**
 *  CustomAdminPrincipal
 *
 * Spring Security의 SecurityContext 안에 저장되는 "인증된 관리자(Admin)" 정보의 최소 표현.
 *
 * 특징:
 * - record 기반 → 불변(immutable) 구조로 안전하게 다룸
 * - DB Admin 엔티티 전체를 들고 다니지 않고, 꼭 필요한 값만 노출:
 *   - id    : 관리자 고유 식별자 (UUID)
 *   - role  : 관리자 권한 (EXCHANGE, CORPORATION, BROKERAGE)
 *   - status: 관리자 상태 (ACTIVE, PENDING, SUSPENDED, DELETED 등)
 *
 * 활용:
 * - 컨트롤러/서비스 단에서 `@AuthenticationPrincipal CustomAdminPrincipal principal` 주입 가능
 *   → 보안 관련 로직에서 직접 헤더 파싱이나 DB 조회할 필요 없이 바로 사용 가능
 *
 *
 */
public record CustomAdminPrincipal(UUID id, Role role, Status status) {

    /**
     * @return 현재 관리자가 ACTIVE 상태인지 여부
     *         - true  → 승인된 관리자
     *         - false → PENDING, SUSPENDED, DELETED 등
     */
    public boolean isActive() {
        return status == Status.ACTIVE;
    }
}
