package com.beyond.MKX.common.auth.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ROLE_EXCHANGE 권한을 가진 관리자만 접근 가능하게 제한하는 커스텀 어노테이션.
 *
 * 사용 예:
 * @ExchangeOnly
 * @GetMapping("/admin/exchange/dashboard")
 * public ApiResponse<?> getExchangeSummary() {
 *     ...
 * }
 *
 * 👉 AccessGuard가 이 어노테이션을 읽고 ROLE_EXCHANGE 가 아닌 경우 403 반환.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('EXCHANGE') and principal != null and @accessGuard.isActiveAdmin(principal.id)")
public @interface ExchangeOnly {
}
