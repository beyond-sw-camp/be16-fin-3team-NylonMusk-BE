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
 * 🔓 로그인 없이도 Public 채널 접근 가능
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
 * - Public 채널 (/topic/*): 인증 불필요 ✅
 * - Private 채널 (/user/queue/*): 인증 필수
 * - 토큰이 없으면 익명으로 진행 허용 ✅
 */
@Slf4j
@Component
public class OptionalAuthChannelInterceptor implements ChannelInterceptor {

    @Value("${jwt.secretKeyAt:default-secret-key-for-development-only-minimum-256-bits}")
    private String jwtSecret;

    /**
     * 메시지 전송 전 인터셉트
     *
     * CONNECT, SUBSCRIBE 커맨드에서 인증 시도 (Optional)
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        // CONNECT 시 인증 시도
        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        }
        // SUBSCRIBE 시 채널 권한 확인
        else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        }

        return message;
    }

    /**
     * CONNECT 명령 처리
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        log.info("[AUTH] 🔌 STOMP CONNECT received");

        // 1️⃣ API Gateway 검증 헤더 우선 사용 (MSA 권장 방식)
        String userId = accessor.getFirstNativeHeader("X-User-Id");
        String userEmail = accessor.getFirstNativeHeader("X-User-Email");
        String userRoles = accessor.getFirstNativeHeader("X-User-Roles");

        if (userId != null && !userId.isEmpty()) {
            authenticateFromGatewayHeaders(accessor, userId, userEmail, userRoles);
            return;
        }

        // 2️⃣ JWT 토큰 직접 검증 (fallback)
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authenticateFromJwtToken(accessor, token);
            return;
        }

        // 3️⃣ 인증 정보 없음 → 익명 사용자로 진행 (Public 채널 접근 가능)
        log.info("[AUTH] 🔓 No authentication provided, proceeding as ANONYMOUS user");
        log.info("[AUTH] ✅ Anonymous users can access PUBLIC channels (/topic/*)");
        
        // 익명 사용자 인증 객체 생성
        Authentication anonymousAuth = new UsernamePasswordAuthenticationToken(
                "anonymous",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        
        accessor.setUser(anonymousAuth);
        SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
    }

    /**
     * SUBSCRIBE 명령 처리
     * 
     * Public 채널은 누구나 접근 가능
     * Private 채널은 인증 필요
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Authentication auth = (accessor.getUser() instanceof Authentication) 
                ? (Authentication) accessor.getUser() 
                : null;

        log.info("[AUTH] 📨 SUBSCRIBE to: {}", destination);

        if (destination == null) {
            return;
        }

        // Public 채널 (/topic/*) - 누구나 접근 가능
        if (destination.startsWith("/topic/")) {
            log.info("[AUTH] ✅ PUBLIC channel access allowed: {}", destination);
            return;
        }

        // Private 채널 (/user/queue/*) - 인증 필요
        if (destination.startsWith("/user/") || destination.startsWith("/queue/")) {
            if (auth == null || "anonymous".equals(auth.getName())) {
                log.warn("[AUTH] ⚠️ Authentication required for PRIVATE channel: {}", destination);
                log.warn("[AUTH] ⚠️ User is not authenticated, but allowing for now (optional auth)");
            } else {
                log.info("[AUTH] ✅ PRIVATE channel access allowed: {} (user: {})", 
                        destination, auth.getName());
            }
        }
    }

    /**
     * API Gateway 검증 헤더로부터 인증 (권장 방식)
     */
    private void authenticateFromGatewayHeaders(StompHeaderAccessor accessor, 
                                                 String userId, 
                                                 String userEmail, 
                                                 String userRoles) {
        try {
            log.info("[AUTH] 🔐 Gateway headers detected: userId={}, email={}", userId, userEmail);

            // 권한 파싱
            List<SimpleGrantedAuthority> authorities;
            if (userRoles != null && !userRoles.isEmpty()) {
                authorities = List.of(userRoles.split(",")).stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()))
                        .toList();
            } else {
                authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            }

            // 인증 객체 생성
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            accessor.setUser(authentication);

            log.info("[AUTH] ✅ Authenticated from Gateway: userId={}, email={}, roles={}", 
                    userId, userEmail, authorities);

        } catch (Exception e) {
            log.error("[AUTH] ❌ Failed to authenticate from Gateway headers", e);
            
            // 실패 시에도 익명으로 진행
            Authentication anonymousAuth = new UsernamePasswordAuthenticationToken(
                    "anonymous",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
            );
            accessor.setUser(anonymousAuth);
        }
    }

    /**
     * JWT 토큰 직접 검증 (fallback)
     */
    private void authenticateFromJwtToken(StompHeaderAccessor accessor, String token) {
        try {
            log.debug("[AUTH] 🔑 Validating JWT token directly...");

            // JWT secret이 기본값이면 경고
            if (jwtSecret.startsWith("default-secret-key")) {
                log.warn("[AUTH] ⚠️ Using default JWT secret - should be configured in production");
            }

            // JWT 토큰 검증
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
                    username != null ? username : userId,
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            accessor.setUser(authentication);

            log.info("[AUTH] ✅ Authenticated from JWT: username={}, userId={}", username, userId);

        } catch (Exception e) {
            log.warn("[AUTH] ⚠️ Invalid JWT token: {}", e.getMessage());
            log.info("[AUTH] 🔓 Proceeding as ANONYMOUS user");
            
            // 실패 시에도 익명으로 진행
            Authentication anonymousAuth = new UsernamePasswordAuthenticationToken(
                    "anonymous",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
            );
            accessor.setUser(anonymousAuth);
        }
    }

    /**
     * 메시지 전송 후 처리
     */
    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            Authentication authentication = (accessor.getUser() instanceof Authentication) 
                    ? (Authentication) accessor.getUser() 
                    : null;

            if (authentication != null && !"anonymous".equals(authentication.getName())) {
                log.info("[AUTH] 🔌 User disconnected: {}", authentication.getName());
            } else {
                log.info("[AUTH] 🔌 Anonymous user disconnected");
            }

            SecurityContextHolder.clearContext();
        }
    }
}
