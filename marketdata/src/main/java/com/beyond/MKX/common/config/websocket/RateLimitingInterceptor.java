package com.beyond.MKX.common.config.websocket;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting 인터셉터
 *
 * 클라이언트별 메시지 전송 속도 제한
 * 초당 100개의 메시지로 제한
 */
@Slf4j
@Component
@SuppressWarnings("UnstableApiUsage")
public class RateLimitingInterceptor implements ChannelInterceptor {

    // 세션별 Rate Limiter
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    // 초당 허용 메시지 수
    private static final double PERMITS_PER_SECOND = 100.0;

    /**
     * 메시지 전송 전 Rate Limit 체크
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            String sessionId = accessor.getSessionId();

            // CONNECT, DISCONNECT, SUBSCRIBE는 Rate Limit 제외
            if (StompCommand.CONNECT.equals(accessor.getCommand()) ||
                    StompCommand.DISCONNECT.equals(accessor.getCommand()) ||
                    StompCommand.SUBSCRIBE.equals(accessor.getCommand()) ||
                    StompCommand.UNSUBSCRIBE.equals(accessor.getCommand())) {
                return message;
            }

            if (sessionId != null) {
                // 세션별 Rate Limiter 생성 (없으면)
                RateLimiter rateLimiter = rateLimiters.computeIfAbsent(
                        sessionId,
                        k -> RateLimiter.create(PERMITS_PER_SECOND)
                );

                // Rate Limit 체크
                if (!rateLimiter.tryAcquire()) {
                    log.warn("[RATE-LIMIT] ⚠️ Rate limit exceeded: sessionId={}, command={}",
                            sessionId, accessor.getCommand());
                    throw new IllegalStateException("Rate limit exceeded");
                }

                log.trace("[RATE-LIMIT] ✅ Message allowed: sessionId={}, command={}",
                        sessionId, accessor.getCommand());
            }
        }

        return message;
    }

    /**
     * 연결 종료 시 Rate Limiter 정리
     * 
     * postSend에서 DISCONNECT를 처리하면 세션이 이미 정리된 후라
     * "No session" 경고가 발생할 수 있습니다.
     * 대신 afterSendCompletion을 사용하거나, 
     * WebSocketEventListener에서 처리하는 것이 더 안전합니다.
     */
    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            String sessionId = accessor.getSessionId();
            if (sessionId != null) {
                RateLimiter removed = rateLimiters.remove(sessionId);
                if (removed != null) {
                    log.debug("[RATE-LIMIT] 🗑️ Removed rate limiter for session: {}", sessionId);
                } else {
                    log.trace("[RATE-LIMIT] ℹ️ No rate limiter found for session: {} (already cleaned)", sessionId);
                }
            }
        }
    }

    /**
     * 활성 Rate Limiter 수 반환 (모니터링용)
     */
    public int getActiveRateLimiterCount() {
        return rateLimiters.size();
    }

    /**
     * 특정 세션의 Rate Limiter 제거 (외부 호출용)
     */
    public void removeRateLimiter(String sessionId) {
        if (sessionId != null) {
            rateLimiters.remove(sessionId);
            log.debug("[RATE-LIMIT] 🗑️ Manually removed rate limiter for session: {}", sessionId);
        }
    }
}