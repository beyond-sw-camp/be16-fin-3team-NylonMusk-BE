package com.beyond.MKX.domain.indicator.websocket;

import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import com.beyond.MKX.domain.websocket.listener.RedisStreamsMessageListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
 * 보조지표 WebSocket Handler (Redis Streams 기반)
 * 
 * 실시간 보조지표 데이터 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    
    @Qualifier("webSocketRedisTemplate")
    private final StringRedisTemplate redisTemplate;
    
    private final RedisStreamsMessageListener streamsListener;

    private static final String STREAM_KEY = "websocket:indicator:stream";
    private final Map<String, CopyOnWriteArraySet<WebSocketSession>> localSessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        streamsListener.registerSessionStore("indicator", localSessions);
        streamsListener.subscribeToStream(STREAM_KEY, "indicator");
        log.info("[INDICATOR-WS] ✅ Initialized with Redis Streams");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String ticker = extractTickerFromSession(session);
        
        if (ticker != null) {
            localSessions.computeIfAbsent(ticker, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("[INDICATOR-WS] ✅ Connected: ticker={}, sessionId={}, localSessionCount={}", 
                    ticker, session.getId(), localSessions.get(ticker).size());
            
            Map<String, Object> response = Map.of(
                    "type", "connected",
                    "ticker", ticker,
                    "message", "Successfully connected to indicator stream",
                    "timestamp", System.currentTimeMillis()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } else {
            log.warn("[INDICATOR-WS] ⚠️ Invalid ticker in URL: {}", session.getUri());
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
                log.info("[INDICATOR-WS] 🔌 Disconnected: ticker={}, sessionId={}, status={}, remainingLocalSessions={}", 
                        ticker, session.getId(), status, sessions.size());
            }
        }
    }

    /**
     * 하트비트: 30초마다 모든 연결된 세션에 ping 전송
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        int totalSent = 0;
        int totalSessions = 0;
        
        for (Map.Entry<String, CopyOnWriteArraySet<WebSocketSession>> entry : localSessions.entrySet()) {
            String ticker = entry.getKey();
            CopyOnWriteArraySet<WebSocketSession> sessions = entry.getValue();
            totalSessions += sessions.size();
            
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        Map<String, Object> ping = Map.of(
                                "type", "ping",
                                "timestamp", System.currentTimeMillis()
                        );
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ping)));
                        totalSent++;
                    } catch (Exception e) {
                        log.warn("[INDICATOR-WS] Failed to send heartbeat: ticker={}, sessionId={}", 
                                ticker, session.getId());
                    }
                } else {
                    sessions.remove(session);
                }
            }
        }
        
        if (totalSessions > 0) {
            log.debug("[INDICATOR-WS] 💓 Heartbeat sent to {}/{} sessions", totalSent, totalSessions);
        }
    }

    /**
     * 보조지표 결과를 Redis Streams로 발행
     */
    public void broadcastIndicator(String ticker, IndicatorResultDTO indicator) {
        try {
            String indicatorJson = objectMapper.writeValueAsString(indicator);
            
            Map<String, String> message = Map.of(
                    "type", "indicator",
                    "ticker", ticker,
                    "data", indicatorJson,
                    "timestamp", String.valueOf(System.currentTimeMillis())
            );
            
            redisTemplate.opsForStream().add(STREAM_KEY, message);
            log.debug("[INDICATOR-WS] 📤 Published to Redis Streams: ticker={}, indicatorType={}, dataPoints={}", 
                    ticker, indicator.getIndicatorType(), indicator.getDataPointCount());
            
        } catch (Exception e) {
            log.error("[INDICATOR-WS] ❌ Failed to publish: ticker={}", ticker, e);
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
            log.error("[INDICATOR-WS] Failed to extract ticker", e);
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
