package com.beyond.MKX.common.config.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * WebSocket Security 설정
 *
 * Spring Boot 3.4.x 호환 버전
 * STOMP WebSocket에 대한 기본 보안 설정
 * 
 * ⚠️ 변경사항:
 * - MessageMatcherDelegatingAuthorizationManager 제거 (Spring Security 6.4에서 deprecated)
 * - 간소화된 HTTP Security 설정
 * - STOMP 메시지 레벨 인증은 OptionalAuthChannelInterceptor에서 처리
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class WebSocketSecurityConfig {

    /**
     * HTTP Security 설정
     *
     * WebSocket 엔드포인트에 대한 접근 제어
     * STOMP 메시지 레벨의 세밀한 인증은 OptionalAuthChannelInterceptor에서 처리
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (WebSocket은 CSRF 토큰 사용 불가)
                .csrf(csrf -> csrf.disable())
                
                // HTTP Basic 인증 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())
                
                // Form 로그인 비활성화
                .formLogin(formLogin -> formLogin.disable())
                
                // Logout 비활성화
                .logout(logout -> logout.disable())
                
                // Anonymous 인증 허용
                .anonymous(anonymous -> anonymous.disable())
                
                // Session 관리 비활성화 (Stateless)
                .sessionManagement(session -> session
                    .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)
                )
                
                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // HTTP 요청 인증 설정
                .authorizeHttpRequests(auth -> auth
                        // ✅ Market Data 공개 API - 인증 불필요 (최우선 순위)
                        .requestMatchers("/api/v1/market/chart/**").permitAll()
                        .requestMatchers("/api/v1/market/integrated/**").permitAll()
                        .requestMatchers("/api/v1/market/price/**").permitAll()
                        .requestMatchers("/api/v1/market/executions/**").permitAll()
                        .requestMatchers("/api/v1/market/**").permitAll()
                        .requestMatchers("/api/v1/executions/**").permitAll()
                        
                        // WebSocket 엔드포인트 허용 (STOMP CONNECT 시 인증)
                        // ✅ /ws/** 패턴으로 모든 WebSocket 경로 커버 (SockJS 포함)
                        .requestMatchers("/ws/**").permitAll()
                        
                        // STOMP 메시지 엔드포인트 허용 (인터셉터에서 처리)
                        .requestMatchers("/app/**").permitAll()
                        .requestMatchers("/topic/**").permitAll()
                        .requestMatchers("/queue/**").permitAll()
                        
                        // Actuator 엔드포인트
                        .requestMatchers("/actuator/**").permitAll()
                        
                        // API 엔드포인트 (나머지)
                        .requestMatchers("/api/**").permitAll()
                        
                        // 기타 모든 요청 허용
                        .anyRequest().permitAll()
                );

        log.info("[SECURITY] ✅ HTTP Security configured");
        log.info("[SECURITY]   - Market Data APIs: /api/v1/market/** (permitAll) 🔓");
        log.info("[SECURITY]   - Execution APIs: /api/v1/executions/** (permitAll) 🔓");
        log.info("[SECURITY]   - WebSocket endpoints: /ws/** (permitAll)");
        log.info("[SECURITY]   - All other APIs: /api/** (permitAll)");
        log.info("[SECURITY]   - STOMP message authorization: handled by OptionalAuthChannelInterceptor");
        
        return http.build();
    }

    /**
     * CORS 설정
     * 
     * WebSocket Upgrade 요청을 위한 CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origin 설정 (✅ allowedOrigins만 사용 - allowedOriginPatterns과 충돌 방지)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:3001",
                "https://www.trading.mk-exchange.shop"
        ));

        // HTTP 메소드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // 헤더 설정
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // WebSocket Upgrade 필수 헤더
        configuration.addAllowedHeader("Upgrade");
        configuration.addAllowedHeader("Connection");
        configuration.addAllowedHeader("Sec-WebSocket-Key");
        configuration.addAllowedHeader("Sec-WebSocket-Version");
        configuration.addAllowedHeader("Sec-WebSocket-Protocol");
        configuration.addAllowedHeader("Sec-WebSocket-Extensions");
        
        // 인증 정보 허용
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("[SECURITY] ✅ CORS configured");
        log.info("[SECURITY]   - Allowed origins: localhost:3000, localhost:3001");
        log.info("[SECURITY]   - Credentials: allowed");
        
        return source;
    }
}
