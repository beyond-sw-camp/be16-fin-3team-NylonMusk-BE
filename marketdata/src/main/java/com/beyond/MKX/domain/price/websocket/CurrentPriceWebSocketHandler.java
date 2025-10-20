package com.beyond.MKX.domain.price.websocket;

import com.beyond.MKX.domain.price.entity.CurrentPrice;
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
 * 현재가 데이터 WebSocket Handler (Redis Streams 기반)
 * 
 * ✅ 수정사항: CurrentPrice 객체를 JSON 문자열로 변환 후 Redis Streams에 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentPriceWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    
    @Qualifier("webSocketRedisTemplate")
    private final StringRedisTemplate redisTemplate;
    
    private final RedisStreamsMessageListener streamsListener;

    private static final String STREAM_KEY = "websocket:price:stream";
    private final Map<String, CopyOnWriteArraySet<WebSocketSession>> localSessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        streamsListener.registerSessionStore("price", localSessions);
        streamsListener.subscribeToStream(STREAM_KEY, "price");
        log.info("[PRICE-WS] ✅ Initialized with Redis Streams");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String ticker = extractTickerFromSession(session);
        
        if (ticker != null) {
            localSessions.computeIfAbsent(ticker, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("[PRICE-WS] ✅ Connected: ticker={}, sessionId={}, localSessionCount={}", 
                    ticker, session.getId(), localSessions.get(ticker).size());
            
            Map<String, Object> response = Map.of(
                    "type", "connected",
                    "ticker", ticker,
                    "message", "Successfully connected to price stream",
                    "timestamp", System.currentTimeMillis()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } else {
            log.warn("[PRICE-WS] ⚠️ Invalid ticker in URL: {}", session.getUri());
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
                log.info("[PRICE-WS] 🔌 Disconnected: ticker={}, sessionId={}, status={}, remainingLocalSessions={}", 
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
                        log.warn("[PRICE-WS] Failed to send heartbeat: ticker={}, sessionId={}", 
                                ticker, session.getId());
                    }
                } else {
                    sessions.remove(session);
                }
            }
        }
        
        if (totalSessions > 0) {
            log.debug("[PRICE-WS] 💓 Heartbeat sent to {}/{} sessions", totalSent, totalSessions);
        }
    }

    /**
     * ✅ 수정사항: CurrentPrice 객체를 JSON 문자열로 변환 후 발행
     */
    public void broadcastPrice(String ticker, CurrentPrice currentPrice) {
        try {
            // ✅ CurrentPrice 객체를 JSON 문자열로 변환
            String priceJson = objectMapper.writeValueAsString(currentPrice);
            
            // 메시지 구성 (모두 String 타입)
            Map<String, String> message = Map.of(
                    "type", "price",
                    "ticker", ticker,
                    "data", priceJson,  // ✅ JSON 문자열로 전달
                    "timestamp", String.valueOf(System.currentTimeMillis())
            );
            
            redisTemplate.opsForStream().add(STREAM_KEY, message);
            log.debug("[PRICE-WS] 📤 Published to Redis Streams: ticker={}, price={}", 
                    ticker, currentPrice.getPrice());
            
        } catch (Exception e) {
            log.error("[PRICE-WS] ❌ Failed to publish: ticker={}", ticker, e);
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
            log.error("[PRICE-WS] Failed to extract ticker", e);
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
