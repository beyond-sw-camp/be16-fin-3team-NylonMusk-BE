package com.beyond.MKX.domain.websocket.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Redis Streams 메시지 리스너 (MapRecord 방식)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamsMessageListener {

    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private final RedisConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;
    
    private final Map<String, Map<String, CopyOnWriteArraySet<WebSocketSession>>> sessionStores 
            = new ConcurrentHashMap<>();
    
    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    
    private static final String CONSUMER_GROUP = "market-data-websocket";
    private String consumerName;

    @PostConstruct
    public void init() {
        consumerName = "consumer-" + System.currentTimeMillis();
        container.start();
        log.info("[REDIS-STREAMS] ✅ Initialized with consumer: {}", consumerName);
    }

    @PreDestroy
    public void destroy() {
        subscriptions.values().forEach(Subscription::cancel);
        if (container != null) {
            container.stop();
        }
        log.info("[REDIS-STREAMS] Stopped");
    }

    /**
     * Stream 구독 시작
     */
    public void subscribeToStream(String streamKey, String type) {
        try {
            // Consumer Group 생성
            try {
                connectionFactory.getConnection()
                        .streamCommands()
                        .xGroupCreate(streamKey.getBytes(), CONSUMER_GROUP, ReadOffset.latest(), true);
                log.info("[REDIS-STREAMS] Created consumer group: stream={}, group={}", streamKey, CONSUMER_GROUP);
            } catch (Exception e) {
                log.debug("[REDIS-STREAMS] Consumer group already exists: stream={}", streamKey);
            }
            
            // Subscription 생성
            Subscription subscription = container.receive(
                    Consumer.from(CONSUMER_GROUP, consumerName),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                    message -> handleMessage(message, type)
            );
            
            subscriptions.put(streamKey, subscription);
            
            log.info("[REDIS-STREAMS] ✅ Subscribed: stream={}, type={}, consumer={}", 
                    streamKey, type, consumerName);
            
        } catch (Exception e) {
            log.error("[REDIS-STREAMS] ❌ Failed to subscribe: stream={}", streamKey, e);
        }
    }

    /**
     * Redis Streams 메시지 처리 (MapRecord)
     */
    @SuppressWarnings("unchecked")
    private void handleMessage(MapRecord<String, String, String> message, String type) {
        try {
            Map<String, String> data = message.getValue();
            
            // 필수 필드 확인
            if (data == null || data.isEmpty()) {
                log.warn("[REDIS-STREAMS] ⚠️ Empty message data: messageId={}", message.getId());
                ackMessage(message);
                return;
            }
            
            String ticker = data.get("ticker");
            if (ticker == null) {
                log.warn("[REDIS-STREAMS] ⚠️ Ticker is null in message: data={}", data);
                ackMessage(message);
                return;
            }
            
            log.debug("[REDIS-STREAMS] 📨 Received: stream={}, type={}, ticker={}, fields={}", 
                    message.getStream(), type, ticker, data.keySet());
            
            // JSON 문자열을 Map으로 변환
            Map<String, Object> jsonData = objectMapper.readValue(
                    objectMapper.writeValueAsString(data), 
                    Map.class
            );
            
            // 타입별 세션 저장소에서 세션 가져오기
            Map<String, CopyOnWriteArraySet<WebSocketSession>> sessions = sessionStores.get(type);
            
            if (sessions != null && ticker != null) {
                CopyOnWriteArraySet<WebSocketSession> tickerSessions = sessions.get(ticker);
                if (tickerSessions != null && !tickerSessions.isEmpty()) {
                    int broadcasted = broadcastToSessions(tickerSessions, jsonData);
                    log.debug("[REDIS-STREAMS] ✅ Broadcasted to {} sessions for ticker={}", broadcasted, ticker);
                } else {
                    log.debug("[REDIS-STREAMS] No active sessions for ticker={}", ticker);
                }
            } else {
                log.debug("[REDIS-STREAMS] No session store found: type={}", type);
            }
            
            // ACK
            ackMessage(message);
            
        } catch (Exception e) {
            log.error("[REDIS-STREAMS] ❌ Failed to process message: messageId={}", message.getId(), e);
            // 에러 발생해도 ACK하여 무한 루프 방지
            ackMessage(message);
        }
    }
    
    /**
     * 메시지 ACK 처리
     */
    private void ackMessage(MapRecord<String, String, String> message) {
        try {
            connectionFactory.getConnection()
                    .streamCommands()
                    .xAck(message.getStream().getBytes(), CONSUMER_GROUP, message.getId());
            log.trace("[REDIS-STREAMS] ACK: messageId={}", message.getId());
        } catch (Exception e) {
            log.error("[REDIS-STREAMS] Failed to ACK message: messageId={}", message.getId(), e);
        }
    }

    /**
     * 로컬 세션들에게 메시지 브로드캐스트
     */
    private int broadcastToSessions(CopyOnWriteArraySet<WebSocketSession> sessions, 
                                     Map<String, Object> data) {
        if (sessions == null || sessions.isEmpty()) {
            log.debug("[REDIS-STREAMS] No local sessions to broadcast");
            return 0;
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(data);
            TextMessage textMessage = new TextMessage(jsonMessage);

            int sentCount = 0;
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        sentCount++;
                    } catch (Exception e) {
                        log.error("[REDIS-STREAMS] Failed to send to session: sessionId={}", 
                                session.getId(), e);
                    }
                }
            }

            return sentCount;

        } catch (Exception e) {
            log.error("[REDIS-STREAMS] Failed to broadcast", e);
            return 0;
        }
    }

    /**
     * 핸들러의 세션 저장소 등록
     */
    public void registerSessionStore(String type, 
                                     Map<String, CopyOnWriteArraySet<WebSocketSession>> sessionStore) {
        sessionStores.put(type, sessionStore);
        log.info("[REDIS-STREAMS] ✅ Registered session store: type={}", type);
    }
}
