package com.beyond.MKX.domain.websocket.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
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
    private final Jackson2JsonRedisSerializer<Object> jsonSerializer;

    /**
     * Redis Pub/Sub 메시지 수신 처리
     *
     * 채널 이름에 따라 적절한 STOMP destination으로 라우팅
     *
     * ✅ 변경사항:
     * - StringRedisTemplate (JSON 문자열) + ObjectMapper 파싱 → RedisTemplate (Jackson2JsonRedisSerializer) + 역직렬화
     * - 체결과 호가 데이터 모두 동일한 방식으로 처리
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            byte[] messageBody = message.getBody();

            log.debug("[REDIS-PUBSUB] 📨 Received: channel={}, bodySize={}", 
                    channel, messageBody.length);

            // ✅ RedisTemplate의 Jackson2JsonRedisSerializer로 직렬화된 데이터를 역직렬화
            // StringRedisTemplate을 사용한 경우 (체결 데이터의 일부 경로)와 
            // RedisTemplate을 사용한 경우 (호가 데이터) 모두 처리 가능
            Map<String, Object> data;
            
            try {
                // 먼저 Jackson2JsonRedisSerializer로 역직렬화 시도 (RedisTemplate 사용 시)
                Object deserialized = jsonSerializer.deserialize(messageBody);
                
                if (deserialized instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> castData = (Map<String, Object>) deserialized;
                    data = castData;
                } else {
                    // 역직렬화 결과가 Map이 아닌 경우 ObjectMapper로 재시도
                    log.warn("[REDIS-PUBSUB] ⚠️ Deserialized object is not a Map: {}", deserialized.getClass());
                    throw new ClassCastException("Deserialized object is not a Map");
                }
            } catch (Exception e) {
                // Jackson2JsonRedisSerializer 역직렬화 실패 시 String으로 변환 후 JSON 파싱 시도 (하위 호환)
                try {
                    String messageBodyStr = new String(messageBody);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsed = objectMapper.readValue(messageBodyStr, Map.class);
                    data = parsed;
                    log.debug("[REDIS-PUBSUB] ✅ Fallback: parsed as JSON string");
                } catch (Exception e2) {
                    log.error("[REDIS-PUBSUB] ❌ Failed to deserialize message: channel={}", channel, e);
                    log.error("[REDIS-PUBSUB] ❌ Fallback JSON parse also failed", e2);
                    return;
                }
            }

            // 채널 타입에 따라 라우팅
            if (channel.startsWith("market:")) {
                routePublicChannel(channel, data);
            } else if (channel.startsWith("user:")) {
                routePrivateChannel(channel, data);
            } else {
                log.warn("[REDIS-PUBSUB] ⚠️ Unknown channel pattern: {}", channel);
            }
            
            log.info("[REDIS-PUBSUB] ✅ Successfully processed message: channel={}, ticker={}", 
                    channel, data.get("ticker"));

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
            
            log.info("[REDIS-PUBSUB] ✅ Sent to public: destination={}, channel={}, ticker={}, type={}", 
                    destination, channel, ticker, data.get("type"));

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
