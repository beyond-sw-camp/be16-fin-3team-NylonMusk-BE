package com.beyond.MKX.domain.execution.websocket;

import com.beyond.MKX.domain.execution.dto.ExecutionEventDTO;
import com.beyond.MKX.domain.websocket.listener.RedisStreamsMessageListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 실시간 체결 데이터 WebSocket Handler (Redis Streams 기반)
 * 
 * 실시간 체결 내역을 클라이언트에게 전송
 * WebSocket 엔드포인트: /ws/execution/{ticker}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisStreamsMessageListener streamsListener;

    private static final String STREAM_KEY = "websocket:execution:stream";
    private final Map<String, CopyOnWriteArraySet<WebSocketSession>> localSessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        streamsListener.registerSessionStore("execution", localSessions);
        streamsListener.subscribeToStream(STREAM_KEY, "execution");
        log.info("[EXECUTION-WS] ✅ Initialized with Redis Streams");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String ticker = extractTickerFromSession(session);
        
        if (ticker != null) {
            localSessions.computeIfAbsent(ticker, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("[EXECUTION-WS] ✅ Connected: ticker={}, sessionId={}, localSessionCount={}", 
                    ticker, session.getId(), localSessions.get(ticker).size());
            
            Map<String, Object> response = Map.of(
                    "type", "connected",
                    "ticker", ticker,
                    "message", "Successfully connected to execution stream",
                    "timestamp", System.currentTimeMillis()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } else {
            log.warn("[EXECUTION-WS] ⚠️ Invalid ticker in URL: {}", session.getUri());
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String ticker = extractTickerFromSession(session);
        
        if (ticker != null) {
            CopyOnWriteArraySet<WebSocketSession> sessions = localSessions.get(ticker);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    localSessions.remove(ticker);
                }
                log.info("[EXECUTION-WS] 🔌 Disconnected: ticker={}, sessionId={}, status={}, remainingLocalSessions={}", 
                        ticker, session.getId(), status, sessions.size());
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("[EXECUTION-WS] Received from client: sessionId={}, message={}", 
                session.getId(), message.getPayload());
    }

    /**
     * 실시간 체결 데이터를 Redis Streams로 발행
     */
    public void broadcastExecution(ExecutionEventDTO execution) {
        try {
            Map<String, Object> message = Map.of(
                    "type", "execution",
                    "ticker", execution.getTicker(),
                    "data", Map.of(
                            "execId", execution.getExecId(),
                            "ticker", execution.getTicker(),
                            "side", execution.getSide(),
                            "price", execution.getPrice(),
                            "quantity", execution.getQuantity(),
                            "timestamp", execution.getTimestamp()
                    ),
                    "timestamp", System.currentTimeMillis()
            );
            
            redisTemplate.opsForStream().add(STREAM_KEY, message);
            log.debug("[EXECUTION-WS] 📤 Published to Redis Streams: ticker={}, price={}, qty={}", 
                    execution.getTicker(), execution.getPrice(), execution.getQuantity());
            
        } catch (Exception e) {
            log.error("[EXECUTION-WS] ❌ Failed to publish: ticker={}", execution.getTicker(), e);
        }
    }

    private String extractTickerFromSession(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri != null) {
                String path = uri.getPath();
                String[] parts = path.split("/");
                if (parts.length >= 4) {
                    return parts[parts.length - 1];
                }
            }
        } catch (Exception e) {
            log.error("[EXECUTION-WS] Failed to extract ticker", e);
        }
        return null;
    }

    public int getLocalSessionCount(String ticker) {
        CopyOnWriteArraySet<WebSocketSession> sessions = localSessions.get(ticker);
        return sessions != null ? sessions.size() : 0;
    }

    public Map<String, Integer> getAllLocalSessionCounts() {
        Map<String, Integer> counts = new ConcurrentHashMap<>();
        localSessions.forEach((ticker, sessions) -> counts.put(ticker, sessions.size()));
        return counts;
    }
}
