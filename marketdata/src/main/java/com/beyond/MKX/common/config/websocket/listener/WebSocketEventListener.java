package com.beyond.MKX.common.config.websocket.listener;

import com.beyond.MKX.common.config.websocket.RateLimitingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * WebSocket 세션 이벤트 리스너
 *
 * WebSocket 연결/해제 이벤트를 처리하고 세션을 추적합니다.
 * "No session" 경고를 방지하기 위한 적절한 세션 관리를 제공합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RateLimitingInterceptor rateLimitingInterceptor;

    /**
     * 활성 세션 추적 맵
     * sessionId -> username
     */
    private final ConcurrentMap<String, String> activeSessions = new ConcurrentHashMap<>();

    /**
     * 세션 구독 정보 추적
     * sessionId -> subscriptionCount
     */
    private final ConcurrentMap<String, Integer> sessionSubscriptions = new ConcurrentHashMap<>();

    /**
     * WebSocket 연결 성공 이벤트
     *
     * CONNECT 명령이 성공적으로 처리된 후 발생
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        Principal principal = headerAccessor.getUser();
        String username = extractUsername(principal);

        // 세션 등록
        activeSessions.put(sessionId, username);
        sessionSubscriptions.put(sessionId, 0);

        log.info("[WS-EVENT] ✅ Client CONNECTED");
        log.info("[WS-EVENT]   - Session ID: {}", sessionId);
        log.info("[WS-EVENT]   - User: {}", username);
        log.info("[WS-EVENT]   - Active sessions: {}", activeSessions.size());
    }

    /**
     * WebSocket 구독 이벤트
     *
     * 클라이언트가 특정 목적지를 구독할 때 발생
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        Principal principal = headerAccessor.getUser();
        String username = extractUsername(principal);

        // 구독 카운트 증가
        sessionSubscriptions.computeIfPresent(sessionId, (key, count) -> count + 1);

        log.info("[WS-EVENT] 📨 Client SUBSCRIBED");
        log.info("[WS-EVENT]   - Session ID: {}", sessionId);
        log.info("[WS-EVENT]   - User: {}", username);
        log.info("[WS-EVENT]   - Destination: {}", destination);
        log.info("[WS-EVENT]   - Subscriptions: {}", sessionSubscriptions.getOrDefault(sessionId, 0));
    }

    /**
     * WebSocket 구독 해제 이벤트
     *
     * 클라이언트가 구독을 취소할 때 발생
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        Principal principal = headerAccessor.getUser();
        String username = extractUsername(principal);

        // 구독 카운트 감소
        sessionSubscriptions.computeIfPresent(sessionId, (key, count) -> Math.max(0, count - 1));

        log.info("[WS-EVENT] 📭 Client UNSUBSCRIBED");
        log.info("[WS-EVENT]   - Session ID: {}", sessionId);
        log.info("[WS-EVENT]   - User: {}", username);
        log.info("[WS-EVENT]   - Remaining subscriptions: {}", sessionSubscriptions.getOrDefault(sessionId, 0));
    }

    /**
     * WebSocket 연결 해제 이벤트
     *
     * 클라이언트 연결이 종료될 때 발생
     * ✅ "No session" 경고를 방지하기 위해 세션 정리를 수행
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // 세션 정보 조회 및 제거
        String username = activeSessions.remove(sessionId);
        Integer subscriptionCount = sessionSubscriptions.remove(sessionId);

        log.info("[WS-EVENT] 🔌 Client DISCONNECTED");
        log.info("[WS-EVENT]   - Session ID: {}", sessionId);
        log.info("[WS-EVENT]   - User: {}", username != null ? username : "unknown");
        log.info("[WS-EVENT]   - Had subscriptions: {}", subscriptionCount != null ? subscriptionCount : 0);
        log.info("[WS-EVENT]   - Active sessions: {}", activeSessions.size());
        log.info("[WS-EVENT]   - Close status: {}", event.getCloseStatus());

        // Rate Limiter 정리 (중복 제거 방지를 위해 명시적 호출)
        if (sessionId != null) {
            rateLimitingInterceptor.removeRateLimiter(sessionId);
        }

        // 세션이 정상적으로 추적되지 않은 경우 경고
        if (username == null) {
            log.warn("[WS-EVENT] ⚠️ Session was not properly tracked: {}", sessionId);
            log.warn("[WS-EVENT] ⚠️ This may cause 'No session' warnings");
        }
    }

    /**
     * Principal에서 사용자 이름 추출
     */
    private String extractUsername(Principal principal) {
        if (principal == null) {
            return "anonymous";
        }

        if (principal instanceof Authentication auth) {
            Object principalObj = auth.getPrincipal();
            if (principalObj instanceof String) {
                return (String) principalObj;
            }
            return auth.getName();
        }

        return principal.getName();
    }

    /**
     * 현재 활성 세션 수 반환
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * 특정 세션이 활성 상태인지 확인
     */
    public boolean isSessionActive(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }

    /**
     * 세션의 사용자 이름 조회
     */
    public String getSessionUsername(String sessionId) {
        return activeSessions.getOrDefault(sessionId, "unknown");
    }
}
