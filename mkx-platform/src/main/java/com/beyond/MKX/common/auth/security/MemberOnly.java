package com.beyond.MKX.common.auth.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ACTIVE 상태의 일반 회원만 접근 가능하도록 제한하는 어노테이션.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('MEMBER') and principal != null and @memberAccessGuard.isActiveMember(principal.id)")
public @interface MemberOnly {
}
