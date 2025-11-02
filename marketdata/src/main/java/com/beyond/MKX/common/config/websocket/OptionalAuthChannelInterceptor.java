package com.beyond.MKX.common.config.websocket;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Optional 인증 채널 인터셉터 (MSA 환경 최적화)
 *
 * MSA 아키텍처에서 API Gateway가 JWT 검증을 수행하는 환경을 고려한 구현
 *
 * 인증 방식:
 * 1. API Gateway 검증 헤더 사용 (권장)
 *    - X-User-Id: 검증된 사용자 ID
 *    - X-User-Email: 검증된 사용자 이메일
 *    - X-User-Roles: 검증된 사용자 권한
 *
 * 2. JWT 토큰 직접 검증 (fallback)
 *    - Authorization: Bearer {token}
 *
 * 인증 정책:
 * - Public 채널 (/topic/*): 인증 불필요
 * - Private 채널 (/user/queue/*): 인증 필수
 * - 토큰이 없거나 유효하지 않아도 익명으로 진행 허용
 */
@Slf4j
@Component
public class OptionalAuthChannelInterceptor implements ChannelInterceptor {

    @Value("${jwt.secretKeyAt}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    /**
     * 메시지 전송 전 인터셉트
     *
     * CONNECT 커맨드에서 인증 시도 (Optional)
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            
            // 1️⃣ API Gateway 검증 헤더 우선 사용 (MSA 권장 방식)
            String userId = accessor.getFirstNativeHeader("X-User-Id");
            String userEmail = accessor.getFirstNativeHeader("X-User-Email");
            String userRoles = accessor.getFirstNativeHeader("X-User-Roles");

            if (userId != null && !userId.isEmpty()) {
                // API Gateway가 이미 검증한 사용자 정보 사용
                authenticateFromGatewayHeaders(accessor, userId, userEmail, userRoles);
                return message;
            }

            // 2️⃣ JWT 토큰 직접 검증 (fallback)
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                authenticateFromJwtToken(accessor, token);
                return message;
            }

            // 3️⃣ 인증 정보 없음 → 익명 사용자로 진행
            log.info("[AUTH] 🔓 No authentication provided, proceeding as anonymous");
            accessor.setUser(null);
        }

        return message;
    }

    /**
     * API Gateway 검증 헤더로부터 인증 (권장 방식)
     *
     * MSA 환경에서 API Gateway가 JWT를 검증하고 전달한 헤더 사용
     */
    private void authenticateFromGatewayHeaders(StompHeaderAccessor accessor, 
                                                 String userId, 
                                                 String userEmail, 
                                                 String userRoles) {
        try {
            log.info("[AUTH] 🔐 Gateway headers detected: userId={}, email={}", userId, userEmail);

            // 권한 파싱 (쉼표 구분)
            List<SimpleGrantedAuthority> authorities;
            if (userRoles != null && !userRoles.isEmpty()) {
                authorities = List.of(userRoles.split(",")).stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()))
                        .toList();
            } else {
                authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            }

            // 인증 객체 생성 (Principal로 userId 사용)
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId,  // Principal: userId
                    null,    // Credentials: 비밀번호 없음
                    authorities
            );

            // SecurityContext에 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
            accessor.setUser(authentication);

            log.info("[AUTH] ✅ Authenticated from Gateway: userId={}, email={}, roles={}", 
                    userId, userEmail, authorities);

        } catch (Exception e) {
            log.warn("[AUTH] ⚠️ Failed to authenticate from Gateway headers: {}", e.getMessage());
            accessor.setUser(null);
        }
    }

    /**
     * JWT 토큰 직접 검증 (fallback)
     *
     * API Gateway 헤더가 없을 때 JWT 토큰을 직접 파싱
     */
    private void authenticateFromJwtToken(StompHeaderAccessor accessor, String token) {
        try {
            log.debug("[AUTH] 🔑 Validating JWT token directly...");

            // JWT 토큰 검증 (0.11.x API)
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            String userId = claims.get("userId", String.class);

            // 인증 객체 생성
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    authorities
            );

            // SecurityContext에 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
            accessor.setUser(authentication);

            log.info("[AUTH] ✅ Authenticated from JWT: username={}, userId={}", username, userId);

        } catch (Exception e) {
            log.warn("[AUTH] ⚠️ Invalid JWT token, proceeding as anonymous: {}", e.getMessage());
            accessor.setUser(null);
        }
    }

    /**
     * 메시지 전송 후 처리
     *
     * DISCONNECT 시 SecurityContext 정리
     */
    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null) {
                log.info("[AUTH] 🔌 User disconnected: {}", authentication.getName());
            } else {
                log.info("[AUTH] 🔌 Anonymous user disconnected");
            }

            SecurityContextHolder.clearContext();
        }
    }
}
