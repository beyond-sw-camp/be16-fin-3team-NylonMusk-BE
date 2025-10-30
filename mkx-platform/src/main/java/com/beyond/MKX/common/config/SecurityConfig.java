package com.beyond.MKX.common.config;

import com.beyond.MKX.common.auth.security.AdminActiveGuardFilter;
import com.beyond.MKX.common.auth.security.GatewayHeaderAuthFilter;
import com.beyond.MKX.common.auth.security.MemberActiveGuardFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import com.beyond.MKX.common.auth.security.RestAccessDeniedHandler;
import com.beyond.MKX.common.auth.security.RestAuthenticationEntryPoint;

/**
 * SecurityConfig
 *
 * 전역 Spring Security 설정
 * - Stateless (세션 안 씀)
 * - 화이트리스트 정의
 * - GatewayHeaderAuthFilter → AdminActiveGuardFilter 순서로 필터 등록
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayHeaderAuthFilter gatewayHeaderAuthFilter;
    private final MemberActiveGuardFilter memberActiveGuardFilter;
    private final AdminActiveGuardFilter adminActiveGuardFilter;

    // 화이트리스트 경로
    private static final String[] WHITELIST = {
            "/auth/**",
            "/admin/approval-requests/me",
            "/api/internal/**",
            "/api/stocks/**",
            "/public/**",
            "/my-stocks",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Gateway에서 이미 CSRF를 검증하므로 내부 서비스는 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // (2) 세션을 쓰지 않고 Stateless 로 동작
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // (3) 권한 부여 규칙
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITELIST).permitAll()   // 화이트리스트는 허용
                        .requestMatchers("/member/**").hasRole("MEMBER")
                        .requestMatchers("/admin/**").hasAnyRole("EXCHANGE", "CORPORATION", "BROKERAGE")
                        .anyRequest().authenticated()             // 나머지는 인증 필요
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint())
                        .accessDeniedHandler(restAccessDeniedHandler())
                )
                // (4) 필터 등록 순서: Gateway → AdminActiveGuardFilter
                .addFilterBefore(gatewayHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(memberActiveGuardFilter, GatewayHeaderAuthFilter.class)
                .addFilterAfter(adminActiveGuardFilter, MemberActiveGuardFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return new RestAuthenticationEntryPoint();
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler() {
        return new RestAccessDeniedHandler();
    }
}
