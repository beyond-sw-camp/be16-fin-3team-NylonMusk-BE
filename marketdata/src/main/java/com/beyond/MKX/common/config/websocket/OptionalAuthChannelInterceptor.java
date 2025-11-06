package com.beyond.MKX.common.config.websocket;

import com.beyond.MKX.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Optional 인증 채널 인터셉터 (개선된 버전)
 *
 * 🔓 로그인 없이도 Public 채널 접근 가능
 *
 * 인증 방식 (우선순위 순):
 * 1. Authorization 헤더 (Bearer token)
 * 2. WebSocket 세션 속성 (쿠키/Gateway 헤더)
 * 3. 익명 사용자 (Public 채널만 접근 가능)
 *
 * 인증 정책:
 * - Public 채널 (/topic/*): 인증 불필요 ✅
 * - Private 채널 (/user/queue/*): 인증 필수 ⚠️
 * - 인증 실패 시 ERROR 프레임 전송 ✅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptionalAuthChannelInterceptor implements ChannelInterceptor {
    
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 메시지 전송 전 인터셉트
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        // CONNECT 프레임 처리
        if (StompCommand.CONNECT.equals(command)) {
            return handleConnect(accessor, message);
        }
        // SUBSCRIBE 프레임 처리
        else if (StompCommand.SUBSCRIBE.equals(command)) {
            return handleSubscribe(accessor, message);
        }

        return message;
    }

    /**
     * CONNECT 명령 처리
     */
    private Message<?> handleConnect(StompHeaderAccessor accessor, Message<?> message) {
        log.info("[STOMP] CONNECT frame received");
        
        // ✅ 1. Authorization 헤더 확인 (추가)
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                // JWT 검증 및 사용자 정보 추출
                String username = jwtTokenProvider.validateAndGetUsername(token);
                
                // Principal 설정 (Spring Security 연동)
                accessor.setUser(new SimplePrincipal(username));
                
                log.info("[STOMP] Authenticated via Authorization header: {}", username);
                
                // ✅ user-name 헤더 설정 (프론트엔드에서 확인 가능)
                accessor.setNativeHeader("user-name", username);
                
                return message;
                
            } catch (Exception e) {
                log.warn("[STOMP] Invalid Authorization token: {}", e.getMessage());
                
                // 인증 실패 시 ERROR 프레임 전송
                StompHeaderAccessor errorAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
                errorAccessor.setMessage("Authentication failed: " + e.getMessage());
                
                return MessageBuilder.createMessage(
                    new byte[0],
                    errorAccessor.getMessageHeaders()
                );
            }
        }
        
        // ✅ 2. 쿠키 기반 인증 확인 (기존 로직 유지)
        // WebSocket 세션 attributes에서 확인
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        
        if (sessionAttributes != null && 
            Boolean.TRUE.equals(sessionAttributes.get("authenticated"))) {
            
            String username = (String) sessionAttributes.get("username");
            accessor.setUser(new SimplePrincipal(username));
            accessor.setNativeHeader("user-name", username);
            
            log.info("[STOMP] Authenticated via session cookie: {}", username);
            return message;
        }
        
        // ✅ 3. 인증 없는 연결 허용 (Public 채널용)
        log.info("[STOMP] Anonymous connection accepted");
        accessor.setNativeHeader("user-name", "anonymous");
        
        return message;
    }

    /**
     * SUBSCRIBE 명령 처리
     */
    private Message<?> handleSubscribe(StompHeaderAccessor accessor, Message<?> message) {
        String destination = accessor.getDestination();
        Principal user = accessor.getUser();
        
        log.info("[STOMP] SUBSCRIBE to: {} by user: {}", 
            destination, 
            user != null ? user.getName() : "anonymous"
        );
        
        // ✅ Private 채널 구독 검증
        if (destination != null && destination.startsWith("/user/queue/")) {
            if (user == null || "anonymous".equals(user.getName())) {
                log.warn("[STOMP] Unauthorized subscription to private channel: {}", destination);
                
                // ERROR 프레임 전송
                StompHeaderAccessor errorAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
                errorAccessor.setMessage("Authentication required for private channels");
                errorAccessor.setDestination(destination);
                
                return MessageBuilder.createMessage(
                    new byte[0],
                    errorAccessor.getMessageHeaders()
                );
            }
        }
        
        // Public 채널은 누구나 구독 가능
        log.info("[STOMP] Subscription allowed");
        return message;
    }

    /**
     * 메시지 전송 후 처리
     */
    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();

            if (user != null && !"anonymous".equals(user.getName())) {
                log.info("[STOMP] 🔌 User disconnected: {}", user.getName());
            } else {
                log.info("[STOMP] 🔌 Anonymous user disconnected");
            }

            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Simple Principal 구현
     */
    private static class SimplePrincipal implements Principal {
        private final String name;
        
        public SimplePrincipal(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
    }
}
