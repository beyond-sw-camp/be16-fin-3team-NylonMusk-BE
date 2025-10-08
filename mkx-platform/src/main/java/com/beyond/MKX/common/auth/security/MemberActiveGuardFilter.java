package com.beyond.MKX.common.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 *  MemberActiveGuardFilter
 *
 * 회원(Member) 전용 보호 필터.
 *  - 인증된 사용자인지 확인
 *  - 회원 상태(MemberStatus)가 ACTIVE인지 확인
 *  - 비활성/정지된 회원은 요청을 차단
 *
 * 실행 시점:
 *  - Spring Security 필터 체인 중 OncePerRequestFilter로, 매 요청마다 단 한 번 동작함.
 *  - GatewayHeaderAuthFilter에서 SecurityContext에 저장된 Authentication 정보를 기반으로 작동.
 */
@Component
public class MemberActiveGuardFilter extends OncePerRequestFilter {

    // URL 패턴 비교용 유틸 (Spring의 Ant-style path matcher)
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    // 인증을 건너뛸 경로(화이트리스트)
    private static final List<String> WHITELIST = List.of(
            "/auth/**"      // 로그인, 회원가입 등 인증 전용 API
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // (1) OPTIONS 메서드(CORS preflight)는 필터링하지 않음
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();

        // (2) 화이트리스트 또는 /member/**가 아닌 요청은 필터링하지 않음
        if (shouldBypass(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // (3) SecurityContext에서 인증 객체(Authentication) 꺼냄
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 없거나 비인증 상태이면 401 Unauthorized 반환
        if (authentication == null || !authentication.isAuthenticated()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증 필요");
            return;
        }

        // (4) Principal 객체 확인 (CustomMemberPrincipal인지 확인)
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomMemberPrincipal memberPrincipal) {

            // 회원 상태가 ACTIVE가 아닐 경우 403 Forbidden 응답
            if (!memberPrincipal.isActive()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                // JSON 형태로 차단 이유 반환
                response.getWriter().write(
                        "{\"code\":\"MEMBER_INACTIVE\",\"message\":\"계정 이용이 정지되었습니다.\"}"
                );
                return;
            }
        }

        // (5) 모든 조건 통과 시 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 화이트리스트 혹은 /member/** 패턴에 해당하지 않으면 true 반환.
     * 즉, 인증이 필요 없는 URI는 통과시킴.
     */
    private boolean shouldBypass(String uri) {
        // /auth/**, /actuator/** 같은 예외 경로는 통과
        if (WHITELIST.stream().anyMatch(pattern -> MATCHER.match(pattern, uri))) {
            return true;
        }
        // 그 외, /member/**가 아닌 모든 요청은 통과 (회원 API만 검사)
        return !MATCHER.match("/member/**", uri);
    }
}
