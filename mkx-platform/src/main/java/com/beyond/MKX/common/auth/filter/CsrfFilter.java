package com.beyond.MKX.common.auth.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Arrays;

/**
 * Double Submit 방식의 CSRF 방어 필터
 * - 쿠키 "CSRF-TOKEN" 값과
 * - 요청 헤더 "X-CSRF-Token" 값이 일치하는지 검증
 *
 * GET, OPTIONS 는 검증하지 않음
 * 불일치 시: 403 Forbidden + ApiResponse 에러 JSON 반환
 */
@Component
public class CsrfFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 로그인/토큰/가입 등 공개 경로는 CSRF 검증 제외
        if (path.startsWith("/auth/login") ||
                path.startsWith("/auth/refresh") ||
                path.startsWith("/auth/logout") ||
                path.startsWith("/auth/signup"))  {
            filterChain.doFilter(request, response);
            return;
        }

        // 안전한 메서드는 패스
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1) 헤더에서 CSRF 토큰 추출
        String csrfHeader = request.getHeader("X-CSRF-Token");

        // 2) 쿠키에서 CSRF-TOKEN 추출
        String csrfCookie = Arrays.stream(request.getCookies() != null ? request.getCookies() : new Cookie[0])
                .filter(c -> "CSRF-TOKEN".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        // 3) 검증
        if (csrfHeader == null || csrfCookie == null || !csrfHeader.equals(csrfCookie)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "CSRF 유효하지 않음");
        }

        // 4) 통과
        filterChain.doFilter(request, response);
    }
}
