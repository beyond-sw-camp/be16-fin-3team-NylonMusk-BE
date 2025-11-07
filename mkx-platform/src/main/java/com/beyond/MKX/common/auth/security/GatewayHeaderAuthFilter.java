package com.beyond.MKX.common.auth.security;

import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.member.repository.MemberRepository;
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
 * Gateway가 붙여준 헤더를 읽어 인증 객체(Authentication)를 SecurityContext에 심는 필터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    private final AdminRepository adminRepository;
    private final MemberRepository memberRepository;

    // 화이트리스트: 인증 체크 없이 통과할 경로들
    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> WHITELIST = List.of(
            "/auth/**",
            "/health"
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
        String userHeader = request.getHeader("X-User-Id");   // 사용자 ID (UUID)
        String roleHeader = request.getHeader("X-User-Role"); // 역할 (관리자: EXCHANGE 등 / 회원: MEMBER)

        // 헤더 없으면 그냥 통과 (인증 없음 → SecurityContext 비어있음)
        if (userHeader == null || roleHeader == null) {
            log.warn("Missing auth headers: user={}, role={}, uri={}", userHeader, roleHeader, uri);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // (3) UUID 변환
            UUID userId = UUID.fromString(userHeader);

            if ("MEMBER".equalsIgnoreCase(roleHeader)) {
                // 회원 인증 처리
                Optional<Member> memberOpt = memberRepository.findById(userId);
                if (memberOpt.isEmpty()) {
                    log.warn("Member not found for id {}", userId);
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                Member member = memberOpt.get();
                CustomMemberPrincipal principal = new CustomMemberPrincipal(member.getId(), member.getStatus());
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // 관리자 인증 처리 (기존 로직)
                Optional<Admin> adminOpt = adminRepository.findById(userId);
                if (adminOpt.isEmpty()) {
                    log.warn("Admin not found for id {}", userId);
                    // DB에 없으면 인증 제거 후 통과
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                Admin admin = adminOpt.get();
                CustomAdminPrincipal principal =
                        new CustomAdminPrincipal(admin.getId(), admin.getRole(), admin.getStatus());

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

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
