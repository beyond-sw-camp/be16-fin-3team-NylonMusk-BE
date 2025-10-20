package com.beyond.MKX.domain.chart.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Redis Pub/Sub 메시지 리스너
 * 
 * Redis로부터 받은 WebSocket 메시지를 로컬 세션들에게 전송
 * MSA 환경에서 각 인스턴스는 자신이 관리하는 세션에만 메시지 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketMessageListener {

    private final ObjectMapper objectMapper;
    
    // 각 핸들러의 로컬 세션 저장소 참조
    // 런타임에 핸들러가 등록됨
    private final Map<String, Map<String, CopyOnWriteArraySet<WebSocketSession>>> sessionStores 
            = new ConcurrentHashMap<>();

    /**
     * Redis 메시지 수신 핸들러
     * MessageListenerAdapter가 호출하는 메서드
     * 
     * @param message Redis 메시지 (JSON String)
     * @param channel Redis 채널명
     */
    public void onMessage(String message, String channel) {
        try {
            log.debug("Received Redis message: channel={}", channel);
            
            // JSON 메시지 파싱
            Map<String, Object> data = objectMapper.readValue(message, 
                    new TypeReference<Map<String, Object>>() {});
            
            // 채널 타입에 따라 적절한 세션으로 라우팅
            if (channel.startsWith("websocket:chart:")) {
                handleChartMessage(data);
            } else if (channel.startsWith("websocket:orderbook:")) {
                handleOrderBookMessage(data);
            } else if (channel.startsWith("websocket:price:")) {
                handlePriceMessage(data);
            }
            
        } catch (Exception e) {
            log.error("Failed to process Redis message: channel={}", channel, e);
        }
    }

    /**
     * 차트 메시지 처리
     */
    private void handleChartMessage(Map<String, Object> data) {
        String ticker = (String) data.get("ticker");
        Map<String, CopyOnWriteArraySet<WebSocketSession>> sessions = sessionStores.get("chart");
        
        if (sessions != null) {
            broadcastToSessions(sessions.get(ticker), data);
        }
    }

    /**
     * 호가 메시지 처리
     */
    private void handleOrderBookMessage(Map<String, Object> data) {
        String ticker = (String) data.get("ticker");
        Map<String, CopyOnWriteArraySet<WebSocketSession>> sessions = sessionStores.get("orderbook");
        
        if (sessions != null) {
            broadcastToSessions(sessions.get(ticker), data);
        }
    }

    /**
     * 현재가 메시지 처리
     */
    private void handlePriceMessage(Map<String, Object> data) {
        String ticker = (String) data.get("ticker");
        Map<String, CopyOnWriteArraySet<WebSocketSession>> sessions = sessionStores.get("price");
        
        if (sessions != null) {
            broadcastToSessions(sessions.get(ticker), data);
        }
    }

    /**
     * 로컬 세션들에게 메시지 브로드캐스트
     */
    private void broadcastToSessions(CopyOnWriteArraySet<WebSocketSession> sessions, 
                                     Map<String, Object> data) {
        if (sessions == null || sessions.isEmpty()) {
            return;
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
                        log.error("Failed to send message to session: sessionId={}", 
                                session.getId(), e);
                    }
                }
            }

            log.debug("Broadcasted message to {} local sessions", sentCount);

        } catch (Exception e) {
            log.error("Failed to broadcast message to sessions", e);
        }
    }

    /**
     * 핸들러의 세션 저장소 등록
     * 각 WebSocket 핸들러가 초기화 시 호출
     * 
     * @param type 핸들러 타입 (chart, orderbook, price)
     * @param sessionStore 해당 핸들러의 세션 저장소
     */
    public void registerSessionStore(String type, 
                                     Map<String, CopyOnWriteArraySet<WebSocketSession>> sessionStore) {
        sessionStores.put(type, sessionStore);
        log.info("Registered session store: type={}", type);
    }
}
