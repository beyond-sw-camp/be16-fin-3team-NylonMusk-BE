package com.beyond.MKX.common.auth.security;

import com.beyond.MKX.domain.admin.entity.Role;
import com.beyond.MKX.domain.admin.entity.Status;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * ActiveGuardFilter
 *
 * 인증된 관리자 계정의 상태(Status)가 ACTIVE 인지 확인하는 필터
 * - PENDING / SUSPENDED / DELETED 상태면 차단
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveGuardFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> WHITELIST = List.of(
            "/auth/**",
            "/actuator/**"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // (1) 화이트리스트는 그냥 통과
        if (isWhitelisted(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // (2) SecurityContext 에서 인증 객체 꺼냄
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            // 인증 자체가 없으면 401
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증 필요");
            return;
        }

        // (3) Principal 꺼내서 상태 확인
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomAdminPrincipal adminPrincipal) {
            // 가입 심사 상태 확인(/admin/approval-requests/me)은 ACTIVE 이전에도 허용
            if (isStatusCheckPath(uri) && isSignUpMember(adminPrincipal)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (adminPrincipal.status() != Status.ACTIVE) {
                // ACTIVE 아니면 403 응답
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"code\":\"ACCOUNT_INACTIVE\",\"message\":\"계정 승인이 필요합니다.\"}"
                );
                return;
            }
        }

        // (4) ACTIVE 계정이면 통과
        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String uri) {
        return WHITELIST.stream().anyMatch(pattern -> MATCHER.match(pattern, uri));
    }

    private boolean isStatusCheckPath(String uri) {
        return MATCHER.match("/**/admin/approval-requests/me", uri);
    }

    private boolean isSignUpMember(CustomAdminPrincipal principal) {
        Role role = principal.role();
        return role == Role.CORPORATION || role == Role.BROKERAGE;
    }
}
