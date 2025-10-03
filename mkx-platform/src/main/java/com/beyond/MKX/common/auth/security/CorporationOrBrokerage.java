package com.beyond.MKX.common.auth.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * ✅ @CorporationOrBrokerage
 *
 * ROLE_CORPORATION 또는 ROLE_BROKERAGE 권한을 가진 관리자만 접근 가능.
 *
 * 사용 예:
 * @CorporationOrBrokerage
 * @PostMapping("/admin/corp-approval")
 * public ApiResponse<?> approveCorporation(@RequestBody ...) {
 *     ...
 * }
 *
 * 👉 AccessGuard에서 Principal.role 이 CORPORATION 또는 BROKERAGE 가 아니면 403 반환.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('CORPORATION','BROKERAGE')")
public @interface CorporationOrBrokerage {
}
