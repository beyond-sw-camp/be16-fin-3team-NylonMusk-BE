package com.beyond.MKX.common.auth.security;

import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GatewayHeaderAuthFilter
 *
 * Gateway가 붙여준 헤더를 읽어 인증 객체(Authentication)를 SecurityContext에 심는 필터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    private final AdminRepository adminRepository;

    // 화이트리스트: 인증 체크 없이 통과할 경로들
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

        // (1) 화이트리스트면 그냥 통과
        if (isWhitelisted(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // (2) 헤더에서 값 꺼내기
        String userHeader = request.getHeader("X-User-Id");   // 관리자 ID (UUID)
        String roleHeader = request.getHeader("X-User-Role"); // 관리자 권한 (EXCHANGE, CORPORATION, BROKERAGE)

        // 헤더 없으면 그냥 통과 (인증 없음 → SecurityContext 비어있음)
        if (userHeader == null || roleHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // (3) UUID 변환
            UUID adminId = UUID.fromString(userHeader);

            // (4) DB 조회
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                // DB에 없으면 인증 제거 후 통과
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // (5) Admin 존재 → Principal 생성
            Admin admin = adminOpt.get();
            CustomAdminPrincipal principal =
                    new CustomAdminPrincipal(admin.getId(), admin.getRole(), admin.getStatus());

            // (6) Authentication 객체 생성 (권한은 ROLE_XXX 형태)
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name()))
            );

            // (7) SecurityContext 에 등록
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (IllegalArgumentException ex) {
            // UUID 변환 실패 시
            log.warn("Invalid X-User-Id header: {}", userHeader, ex);
            SecurityContextHolder.clearContext();
        }

        // (8) 다음 필터 진행
        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String uri) {
        return WHITELIST.stream().anyMatch(pattern -> MATCHER.match(pattern, uri));
    }
}

