package com.beyond.MKX.common.auth.security;

import com.beyond.MKX.domain.admin.entity.Status;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AdminAccessGuard
 *
 * 커스텀 어노테이션(@ExchangeOnly, @CorporationOrBrokerage 등)에서 재사용할 관리자 권한/상태 검증 로직.
 * MethodSecurity로 실행돼 메서드 단위 인가를 담당한다.
 */
@Component("adminAccessGuard")
@RequiredArgsConstructor
public class AdminAccessGuard {

    private final AdminRepository adminRepository;

    /** 관리자 ID가 존재하고 특정 역할인지 확인 */
    public boolean hasRole(UUID adminId, String requiredRole) {
        return adminRepository.findById(adminId)
                .map(admin -> admin.getRole().name().equalsIgnoreCase(requiredRole))
                .orElse(false);
    }

    /** 관리자 상태가 ACTIVE인지 확인 */
    public boolean isActiveAdmin(UUID adminId) {
        return adminRepository.findById(adminId)
                .map(admin -> admin.getStatus() == Status.ACTIVE)
                .orElse(false);
    }

    /** 역할과 상태(ACTIVE)를 동시에 검증 */
    public boolean hasActiveRole(UUID adminId, String requiredRole) {
        return adminRepository.findById(adminId)
                .map(admin -> admin.getStatus() == Status.ACTIVE
                        && admin.getRole().name().equalsIgnoreCase(requiredRole))
                .orElse(false);
    }
}
