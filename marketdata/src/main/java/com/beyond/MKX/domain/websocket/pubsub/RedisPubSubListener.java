package com.beyond.MKX.domain.websocket.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis Pub/Sub 메시지 리스너
 *
 * Redis로부터 발행된 메시지를 수신하여 STOMP를 통해 WebSocket 클라이언트에게 전달
 *
 * 채널 구조:
 * - market:orderbook → /topic/orderbook/{ticker}
 * - market:trades → /topic/trades/{ticker}
 * - market:price → /topic/price/{ticker}
 * - market:chart → /topic/chart/{ticker}
 * - market:indicator → /topic/indicator/{ticker}
 * - market:summary → /topic/market-summary
 * - user:{userId}:* → /user/{userId}/queue/*
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPubSubListener implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis Pub/Sub 메시지 수신 처리
     *
     * 채널 이름에 따라 적절한 STOMP destination으로 라우팅
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String messageBody = new String(message.getBody());

            log.debug("[REDIS-PUBSUB] 📨 Received: channel={}, bodySize={}", 
                    channel, messageBody.length());

            // JSON 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(messageBody, Map.class);

            // 채널 타입에 따라 라우팅
            if (channel.startsWith("market:")) {
                routePublicChannel(channel, data);
            } else if (channel.startsWith("user:")) {
                routePrivateChannel(channel, data);
            } else {
                log.warn("[REDIS-PUBSUB] ⚠️ Unknown channel pattern: {}", channel);
            }

        } catch (Exception e) {
            log.error("[REDIS-PUBSUB] ❌ Failed to process message", e);
        }
    }

    /**
     * Public 채널 라우팅 (인증 불필요)
     *
     * market:{type} → /topic/{type}/{ticker}
     */
    private void routePublicChannel(String channel, Map<String, Object> data) {
        try {
            String ticker = (String) data.get("ticker");
            String destination;

            switch (channel) {
                case "market:orderbook" -> 
                    destination = "/topic/orderbook/" + ticker;
                case "market:trades" -> 
                    destination = "/topic/trades/" + ticker;
                case "market:price" -> 
                    destination = "/topic/price/" + ticker;
                case "market:chart" -> 
                    destination = "/topic/chart/" + ticker;
                case "market:indicator" -> 
                    destination = "/topic/indicator/" + ticker;
                case "market:summary" -> 
                    destination = "/topic/market-summary";
                default -> {
                    log.warn("[REDIS-PUBSUB] ⚠️ Unknown public channel: {}", channel);
                    return;
                }
            }

            messagingTemplate.convertAndSend(destination, data);
            
            log.debug("[REDIS-PUBSUB] ✅ Sent to public: destination={}, channel={}, ticker={}", 
                    destination, channel, ticker);

        } catch (Exception e) {
            log.error("[REDIS-PUBSUB] ❌ Failed to route public channel: channel={}", 
                    channel, e);
        }
    }

    /**
     * Private 채널 라우팅 (인증 필수)
     *
     * user:{userId}:{type} → /user/{userId}/queue/{type}
     */
    private void routePrivateChannel(String channel, Map<String, Object> data) {
        try {
            // user:{userId}:{type} 형식에서 파싱
            String[] parts = channel.split(":", 3);
            if (parts.length < 3) {
                log.warn("[REDIS-PUBSUB] ⚠️ Invalid private channel format: {}", channel);
                return;
            }

            String userId = parts[1];
            String type = parts[2];

            // 메시지에서도 userId 추출 (검증용)
            String dataUserId = (String) data.get("userId");
            if (dataUserId != null && !userId.equals(dataUserId)) {
                log.warn("[REDIS-PUBSUB] ⚠️ UserId mismatch: channel={}, data={}", 
                        userId, dataUserId);
            }

            // STOMP destination 생성
            String destination = "/queue/" + type;

            // 특정 사용자에게 전송
            messagingTemplate.convertAndSendToUser(userId, destination, data);
            
            log.debug("[REDIS-PUBSUB] ✅ Sent to private: userId={}, destination={}, type={}", 
                    userId, destination, type);

        } catch (Exception e) {
            log.error("[REDIS-PUBSUB] ❌ Failed to route private channel: channel={}", 
                    channel, e);
        }
    }
}
