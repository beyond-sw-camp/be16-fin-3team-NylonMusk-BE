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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Redis Streams 메시지 리스너 (MapRecord 방식)
 * 
 * ✅ 수정사항: 
 * - Consumer Group 방식에서 각 인스턴스가 독립적으로 구독하는 방식으로 변경
 * - 모든 인스턴스가 모든 메시지를 받아서 로컬 세션에 브로드캐스트
 */
@Slf4j
@Component
public class RedisStreamsMessageListener {

    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private final RedisConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;
    
    private final Map<String, Map<String, CopyOnWriteArraySet<WebSocketSession>>> sessionStores 
            = new ConcurrentHashMap<>();
    
    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    
    // ✅ 각 애플리케이션 인스턴스마다 고유한 Consumer Group 사용
    private final String CONSUMER_GROUP;
    private final String consumerName;

    // ✅ Spring이 자동으로 의존성 주입할 수 있도록 생성자 만들기
    public RedisStreamsMessageListener(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        this.container = container;
        this.connectionFactory = connectionFactory;
        this.objectMapper = objectMapper;
        
        // ✅ 애플리케이션 인스턴스마다 고유한 Consumer Group 생성
        String instanceId = UUID.randomUUID().toString();
        this.CONSUMER_GROUP = "market-data-websocket-" + instanceId;
        this.consumerName = "consumer-" + instanceId;
        
        log.info("[REDIS-STREAMS] 🆔 Instance ID: {}, Consumer Group: {}, Consumer: {}", 
                instanceId, CONSUMER_GROUP, consumerName);
    }

    @PostConstruct
    public void init() {
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
     * 
     * ✅ 각 애플리케이션 인스턴스가 독립적인 Consumer Group을 사용하여
     *    모든 메시지를 받을 수 있도록 구현
     */
    public void subscribeToStream(String streamKey, String type) {
        try {
            // ✅ Consumer Group 생성 (인스턴스별 고유)
            try {
                connectionFactory.getConnection()
                        .streamCommands()
                        .xGroupCreate(streamKey.getBytes(), CONSUMER_GROUP, ReadOffset.latest(), true);
                log.info("[REDIS-STREAMS] Created consumer group: stream={}, group={}", streamKey, CONSUMER_GROUP);
            } catch (Exception e) {
                log.debug("[REDIS-STREAMS] Consumer group already exists: stream={}, group={}", streamKey, CONSUMER_GROUP);
            }
            
            // ✅ Subscription 생성 - 각 인스턴스가 모든 메시지를 받음
            Subscription subscription = container.receive(
                    Consumer.from(CONSUMER_GROUP, consumerName),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                    message -> handleMessage(message, type)
            );
            
            subscriptions.put(streamKey, subscription);
            
            log.info("[REDIS-STREAMS] ✅ Subscribed: stream={}, type={}, consumer={}, group={}", 
                    streamKey, type, consumerName, CONSUMER_GROUP);
            
        } catch (Exception e) {
            log.error("[REDIS-STREAMS] ❌ Failed to subscribe: stream={}", streamKey, e);
        }
    }

    /**
     * Redis Streams 메시지 처리 (MapRecord)
     * 
     * ✅ 이 메서드는 각 애플리케이션 인스턴스에서 실행되며,
     *    로컬에 연결된 WebSocket 세션들에게만 메시지를 전송
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
            
            // ⚠️ ticker에 인용부호가 포함되어 있는지 확인
            if (ticker.startsWith("\"") && ticker.endsWith("\"")) {
                String cleanedTicker = ticker.substring(1, ticker.length() - 1);
                log.warn("[REDIS-STREAMS] ⚠️ Ticker has quotes! Original: '{}', Cleaned: '{}'", ticker, cleanedTicker);
                ticker = cleanedTicker;
            }
            
            log.info("[REDIS-STREAMS] 📨 Received: stream={}, type={}, ticker='{}', messageId={}, consumerGroup={}, fields={}", 
                    message.getStream(), type, ticker, message.getId(), CONSUMER_GROUP, data.keySet());
            
            // JSON 문자열을 Map으로 변환
            Map<String, Object> jsonData = objectMapper.readValue(
                    objectMapper.writeValueAsString(data), 
                    Map.class
            );
            
            // ✅ 타입별 세션 저장소에서 로컬 세션 가져오기
            Map<String, CopyOnWriteArraySet<WebSocketSession>> sessions = sessionStores.get(type);
            
            log.debug("[REDIS-STREAMS] Session store lookup: type={}, hasStore={}, ticker={}", 
                    type, sessions != null, ticker);
            
            if (sessions != null && ticker != null) {
                CopyOnWriteArraySet<WebSocketSession> tickerSessions = sessions.get(ticker);
                
                log.info("[REDIS-STREAMS] Ticker sessions lookup: ticker='{}', found={}, count={}", 
                        ticker, tickerSessions != null, tickerSessions != null ? tickerSessions.size() : 0);
                
                // ✅ 모든 ticker 키 출력
                log.info("[REDIS-STREAMS] Available tickers in session store: {}", sessions.keySet());
                
                if (tickerSessions != null && !tickerSessions.isEmpty()) {
                    int broadcasted = broadcastToSessions(tickerSessions, jsonData);
                    log.info("[REDIS-STREAMS] ✅ Broadcasted to {} LOCAL sessions for ticker={}, type={}, consumerGroup={}", 
                            broadcasted, ticker, type, CONSUMER_GROUP);
                } else {
                    log.info("[REDIS-STREAMS] ⚠️ No active LOCAL sessions for ticker={}, type={}, consumerGroup={}", 
                            ticker, type, CONSUMER_GROUP);
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
            log.trace("[REDIS-STREAMS] ACK: messageId={}, group={}", message.getId(), CONSUMER_GROUP);
        } catch (Exception e) {
            log.error("[REDIS-STREAMS] Failed to ACK message: messageId={}, group={}", 
                    message.getId(), CONSUMER_GROUP, e);
        }
    }

    /**
     * 로컬 세션들에게 메시지 브로드캐스트
     * 
     * ✅ 이 인스턴스에 연결된 WebSocket 세션들에게만 전송
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
                        log.debug("[REDIS-STREAMS] 📤 Sent to session: sessionId={}, remoteAddr={}", 
                                session.getId(), session.getRemoteAddress());
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
        log.info("[REDIS-STREAMS] ✅ Registered session store: type={}, consumerGroup={}", type, CONSUMER_GROUP);
    }
}
