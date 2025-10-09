package com.beyond.MKX.common.auth.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ExchangeOrBrokerage
 *
 * ROLE_EXCHANGE 또는 ROLE_BROKERAGE 이면서 활성화(ACTIVE)된 관리자만 접근 허용.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('EXCHANGE','BROKERAGE') and principal != null and @adminAccessGuard.isActiveAdmin(principal.id)")
public @interface ExchangeOrBrokerage {
}
