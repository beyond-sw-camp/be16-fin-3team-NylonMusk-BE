package com.beyond.MKX.domain.chart.websocket;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.websocket.listener.RedisStreamsMessageListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
 * 차트 데이터 WebSocket Handler (Redis Streams 기반)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChartWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisStreamsMessageListener streamsListener;

    private static final String STREAM_KEY = "websocket:chart:stream";
    private final Map<String, CopyOnWriteArraySet<WebSocketSession>> localSessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        streamsListener.registerSessionStore("chart", localSessions);
        streamsListener.subscribeToStream(STREAM_KEY, "chart");
        log.info("[CHART-WS] ✅ Initialized with Redis Streams");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String ticker = extractTickerFromSession(session);
        
        if (ticker != null) {
            localSessions.computeIfAbsent(ticker, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("[CHART-WS] ✅ Connected: ticker={}, sessionId={}, localSessionCount={}", 
                    ticker, session.getId(), localSessions.get(ticker).size());
            
            Map<String, Object> response = Map.of(
                    "type", "connected",
                    "ticker", ticker,
                    "message", "Successfully connected to chart stream",
                    "timestamp", System.currentTimeMillis()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } else {
            log.warn("[CHART-WS] ⚠️ Invalid ticker in URL: {}", session.getUri());
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
                log.info("[CHART-WS] 🔌 Disconnected: ticker={}, sessionId={}, status={}, remainingLocalSessions={}", 
                        ticker, session.getId(), status, sessions.size());
            }
        }
    }

    /**
     * ✅ 하트비트: 30초마다 모든 연결된 세션에 ping 전송
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
                        log.warn("[CHART-WS] Failed to send heartbeat: ticker={}, sessionId={}", 
                                ticker, session.getId());
                    }
                } else {
                    sessions.remove(session);
                }
            }
        }
        
        if (totalSessions > 0) {
            log.debug("[CHART-WS] 💓 Heartbeat sent to {}/{} sessions", totalSent, totalSessions);
        }
    }

    public void broadcastCandle(String ticker, Candle candle) {
        try {
            Map<String, Object> message = Map.of(
                    "type", "candle",
                    "ticker", ticker,
                    "data", candle,
                    "timestamp", System.currentTimeMillis()
            );
            
            redisTemplate.opsForStream().add(STREAM_KEY, message);
            log.debug("[CHART-WS] 📤 Published to Redis Streams: ticker={}, interval={}, volume={}", 
                    ticker, candle.getInterval(), candle.getVolume());
            
        } catch (Exception e) {
            log.error("[CHART-WS] ❌ Failed to publish: ticker={}", ticker, e);
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
            log.error("[CHART-WS] Failed to extract ticker", e);
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
