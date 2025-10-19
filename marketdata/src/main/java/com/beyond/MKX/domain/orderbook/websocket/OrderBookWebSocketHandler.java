package com.beyond.MKX.domain.orderbook.websocket;

import com.beyond.MKX.domain.websocket.listener.RedisStreamsMessageListener;
import com.beyond.MKX.domain.orderbook.entity.OrderBook;
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
 * 호가 데이터 WebSocket Handler (Redis Streams 기반)
 * 
 * 실시간 호가 데이터를 클라이언트에게 전송
 * MSA 환경에서 여러 인스턴스 간 메시지 공유를 위해 Redis Streams 사용
 * 
 * ✅ 수정사항: OrderBook 객체를 JSON 문자열로 변환 후 Redis Streams에 발행
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
                        log.warn("[ORDERBOOK-WS] Failed to send heartbeat: ticker={}, sessionId={}", 
                                ticker, session.getId());
                    }
                } else {
                    sessions.remove(session);
                }
            }
        }
        
        if (totalSessions > 0) {
            log.debug("[ORDERBOOK-WS] 💓 Heartbeat sent to {}/{} sessions", totalSent, totalSessions);
        }
    }

    /**
     * 특정 종목의 호가 데이터를 Redis Streams로 발행
     * 
     * ✅ 수정사항: OrderBook 객체를 JSON 문자열로 변환 후 발행
     */
    public void broadcastOrderBook(String ticker, OrderBook orderBook) {
        try {
            // ✅ OrderBook 객체를 JSON 문자열로 변환
            String orderBookJson = objectMapper.writeValueAsString(orderBook);
            
            // 메시지 구성 (모두 String 타입)
            Map<String, String> message = Map.of(
                    "type", "orderbook",
                    "ticker", ticker,
                    "data", orderBookJson,  // ✅ JSON 문자열로 전달
                    "timestamp", String.valueOf(System.currentTimeMillis())
            );
            
            // Redis Streams로 발행
            redisTemplate.opsForStream().add(STREAM_KEY, message);
            
            log.debug("[ORDERBOOK-WS] 📤 Published to Redis Streams: ticker={}, dataSize={}, bids={}, asks={}", 
                    ticker, orderBookJson.length(), orderBook.getBids().size(), orderBook.getAsks().size());
            
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
