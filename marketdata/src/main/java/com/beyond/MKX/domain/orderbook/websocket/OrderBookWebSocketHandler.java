package com.beyond.MKX.domain.orderbook.websocket;

import com.beyond.MKX.domain.websocket.listener.RedisStreamsMessageListener;
import com.beyond.MKX.domain.orderbook.entity.OrderBook;
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
 * 호가 데이터 WebSocket Handler (Redis Streams 기반)
 * 
 * 실시간 호가 데이터를 클라이언트에게 전송
 * MSA 환경에서 여러 인스턴스 간 메시지 공유를 위해 Redis Streams 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderBookWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisStreamsMessageListener streamsListener;

    // Redis Stream key
    private static final String STREAM_KEY = "websocket:orderbook:stream";

    // 로컬 인스턴스의 ticker별 WebSocket 세션 관리
    private final Map<String, CopyOnWriteArraySet<WebSocketSession>> localSessions 
            = new ConcurrentHashMap<>();

    /**
     * 초기화: Redis Streams 구독 시작
     */
    @PostConstruct
    public void init() {
        // 세션 저장소 등록
        streamsListener.registerSessionStore("orderbook", localSessions);
        
        // Redis Streams 구독 시작
        streamsListener.subscribeToStream(STREAM_KEY, "orderbook");
        
        log.info("[ORDERBOOK-WS] ✅ Initialized with Redis Streams");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String ticker = extractTickerFromSession(session);
        
        if (ticker != null) {
            localSessions.computeIfAbsent(ticker, k -> new CopyOnWriteArraySet<>()).add(session);
            
            int sessionCount = localSessions.get(ticker).size();
            
            log.info("[ORDERBOOK-WS] ✅ Connected: ticker={}, sessionId={}, localSessionCount={}", 
                    ticker, session.getId(), sessionCount);
            
            // 연결 성공 메시지 전송
            Map<String, Object> response = Map.of(
                    "type", "connected",
                    "ticker", ticker,
                    "message", "Successfully connected to orderbook stream",
                    "timestamp", System.currentTimeMillis()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } else {
            log.warn("[ORDERBOOK-WS] ⚠️ Invalid ticker in URL: {}", session.getUri());
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
                
                log.info("[ORDERBOOK-WS] 🔌 Disconnected: ticker={}, sessionId={}, status={}, remainingLocalSessions={}", 
                        ticker, session.getId(), status, sessions.size());
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("[ORDERBOOK-WS] Received from client: sessionId={}, message={}", 
                session.getId(), message.getPayload());
    }

    /**
     * 특정 종목의 호가 데이터를 Redis Streams로 발행
     */
    public void broadcastOrderBook(String ticker, OrderBook orderBook) {
        try {
            // 메시지 구성
            Map<String, Object> message = Map.of(
                    "type", "orderbook",
                    "ticker", ticker,
                    "data", orderBook,
                    "timestamp", System.currentTimeMillis()
            );
            
            // Redis Streams로 발행
            redisTemplate.opsForStream().add(STREAM_KEY, message);
            
            log.debug("[ORDERBOOK-WS] 📤 Published to Redis Streams: ticker={}", ticker);
            
        } catch (Exception e) {
            log.error("[ORDERBOOK-WS] ❌ Failed to publish: ticker={}", ticker, e);
        }
    }

    /**
     * WebSocket URI에서 ticker 추출
     */
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
            log.error("[ORDERBOOK-WS] Failed to extract ticker", e);
        }
        return null;
    }

    /**
     * 로컬 세션 수 조회
     */
    public int getLocalSessionCount(String ticker) {
        CopyOnWriteArraySet<WebSocketSession> sessions = localSessions.get(ticker);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * 모든 종목의 로컬 세션 수 조회
     */
    public Map<String, Integer> getAllLocalSessionCounts() {
        Map<String, Integer> counts = new ConcurrentHashMap<>();
        localSessions.forEach((ticker, sessions) -> 
                counts.put(ticker, sessions.size()));
        return counts;
    }
}
