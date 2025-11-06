package com.beyond.MKX.common.config.websocket;

import com.beyond.MKX.common.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket Handshake 인터셉터 - 다중 인증 지원
 *
 * 인증 방식 (우선순위 순):
 * 1. HTTP 쿠키 (AT: Access Token)
 * 2. API Gateway 검증 헤더 (X-User-Id, X-User-Email, X-User-Role)
 * 3. Authorization 헤더는 STOMP CONNECT 프레임에서 확인 (OptionalAuthChannelInterceptor)
 *
 * 전달되는 속성:
 * - username: 사용자명
 * - authenticated: 인증 여부 (true/false)
 * - authMethod: 인증 방법 (cookie/gateway/none)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayAuthHandshakeInterceptor implements HandshakeInterceptor {
    
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * WebSocket Handshake 전 처리
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        log.info("[WebSocket] Handshake started");

        // ✅ 1. HTTP 쿠키에서 인증 확인 (기존 로직)
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            
            // 쿠키에서 액세스 토큰 확인
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("AT".equals(cookie.getName())) {
                        String accessToken = cookie.getValue();
                        
                        try {
                            // JWT 검증 및 사용자 정보 추출
                            String username = jwtTokenProvider.validateAndGetUsername(accessToken);
                            
                            attributes.put("username", username);
                            attributes.put("authenticated", true);
                            attributes.put("authMethod", "cookie");
                            
                            log.info("[WebSocket] Authenticated via cookie: {}", username);
                            return true;
                            
                        } catch (Exception e) {
                            log.warn("[WebSocket] Invalid access token in cookie: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        // ✅ 2. API Gateway 검증 헤더에서 인증 확인
        String userId = extractHeader(request, "X-User-Id");
        String userEmail = extractHeader(request, "X-User-Email");
        String userRole = extractHeader(request, "X-User-Role");

        if (userId != null && !userId.isEmpty()) {
            attributes.put("username", userId);
            attributes.put("X-User-Id", userId);
            attributes.put("X-User-Email", userEmail != null ? userEmail : "");
            attributes.put("X-User-Role", userRole != null ? userRole : "USER");
            attributes.put("authenticated", true);
            attributes.put("authMethod", "gateway");
            
            log.info("[WebSocket] Authenticated via gateway headers");
            log.info("[WebSocket]   - User ID: {}", userId);
            log.info("[WebSocket]   - Email: {}", userEmail);
            log.info("[WebSocket]   - Role: {}", userRole);
            return true;
        }
        
        // ✅ 3. Authorization 헤더는 STOMP CONNECT 프레임에서 확인 (추가)
        // STOMP CONNECT 프레임의 헤더는 여기서 확인할 수 없음
        // StompHeaderAccessor를 사용해야 함 (OptionalAuthChannelInterceptor에서 처리)
        
        // 인증 실패해도 연결은 허용 (Public 채널 접근 위해)
        attributes.put("authenticated", false);
        attributes.put("authMethod", "none");
        
        log.info("[WebSocket] Anonymous connection allowed (for public channels)");
        return true;
    }

    /**
     * WebSocket Handshake 후 처리
     */
    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {

        if (exception != null) {
            log.error("[WebSocket] Handshake failed", exception);
        } else {
            log.info("[WebSocket] Handshake completed successfully");
        }
    }

    /**
     * HTTP 헤더 추출 유틸리티
     */
    private String extractHeader(ServerHttpRequest request, String headerName) {
        if (request.getHeaders().containsKey(headerName)) {
            return request.getHeaders().getFirst(headerName);
        }
        return null;
    }
}
