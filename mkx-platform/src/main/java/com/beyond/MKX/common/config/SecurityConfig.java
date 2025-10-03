package com.beyond.MKX.common.config;

import com.beyond.MKX.common.auth.security.ActiveGuardFilter;
import com.beyond.MKX.common.auth.security.GatewayHeaderAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig
 *
 * 전역 Spring Security 설정
 * - Stateless (세션 안 씀)
 * - 화이트리스트 정의
 * - GatewayHeaderAuthFilter → ActiveGuardFilter 순서로 필터 등록
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayHeaderAuthFilter gatewayHeaderAuthFilter;
    private final ActiveGuardFilter activeGuardFilter;

    // 화이트리스트 경로
    private static final String[] WHITELIST = {
            "/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**",
            "/admin/approval-requests/me"
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
                        .anyRequest().authenticated()             // 나머지는 인증 필요
                )
                // (4) 필터 등록 순서: Gateway → ActiveGuard
                .addFilterBefore(gatewayHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(activeGuardFilter, GatewayHeaderAuthFilter.class);

        return http.build();
    }

    // 패스워드 인코더 (BCrypt 기본)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
