package com.beyond.MKX.common.auth.security;

import com.beyond.MKX.domain.admin.entity.Status;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 커스텀 어노테이션(@ExchangeOnly, @CorporationOrBrokerage 등)에서 재사용할 검증 로직
 * 현재 로그인한 Admin의 권한(role, status)을 검사하는 클래스.
 *
 * 보통 AOP(@Aspect) 방식으로 동작하거나, Spring Security MethodSecurity 에서 해석되어 실행됨.
 *
 * 흐름:
 * 1. 컨트롤러/서비스 메서드 실행 전에 동작
 * 2. SecurityContext에서 Authentication 꺼내고 → Principal 캐스팅(CustomAdminPrincipal)
 * 3. Principal.role / Principal.status 확인
 * 4. 조건 안 맞으면 AccessDeniedException 발생 → 403 Forbidden 반환
 *
 * 👉 메서드 단위의 "세부 권한 체크"를 가능하게 함.
 */
@Component("accessGuard")
@RequiredArgsConstructor
public class AccessGuard {

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
