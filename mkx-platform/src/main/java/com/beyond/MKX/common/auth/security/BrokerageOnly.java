package com.beyond.MKX.common.auth.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 증권사 관리자(BROKERAGE) 역할이면서 ACTIVE 상태인지 확인하는 어노테이션.
 * 컨트롤러/핸들러에 붙여 "승인된 증권사 관리자만 접근"을 강제한다.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('BROKERAGE') and principal != null and @adminAccessGuard.isActiveAdmin(principal.id)")
public @interface BrokerageOnly {
}
