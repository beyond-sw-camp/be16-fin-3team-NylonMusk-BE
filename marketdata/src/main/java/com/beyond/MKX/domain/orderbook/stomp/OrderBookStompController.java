package com.beyond.MKX.domain.orderbook.stomp;

import com.beyond.MKX.domain.orderbook.dto.enhanced.EnhancedOrderBookDTO;
import com.beyond.MKX.domain.orderbook.event.OrderBookUpdateEvent;
import com.beyond.MKX.domain.orderbook.service.EnhancedOrderBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 호가 STOMP 컨트롤러
 *
 * OrderBook 업데이트 이벤트를 수신하여 Redis Pub/Sub으로 발행
 * 채널: market:orderbook
 * STOMP 구독자들은 RedisPubSubListener를 통해 /topic/orderbook/{ticker}로 수신
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class OrderBookStompController {

    private final EnhancedOrderBookService enhancedOrderBookService;
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis Pub/Sub 채널명 (간소화)
    private static final String REDIS_CHANNEL = "market:orderbook";

    /**
     * 초기 구독 시 호가 데이터 즉시 전송
     *
     * 클라이언트가 /topic/orderbook/{ticker}를 구독하면 즉시 호가 데이터를 반환
     * 이후 업데이트는 Redis Pub/Sub을 통해 전송됨
     *
     * @param ticker 종목 코드
     * @return 호가 데이터 메시지
     */
    @SubscribeMapping("/topic/orderbook/{ticker}")
    public Map<String, Object> onSubscribe(@DestinationVariable String ticker) {
        log.info("[ORDERBOOK-STOMP] 🔔 New subscription: ticker={}", ticker);

        try {
            // 고도화된 호가 데이터 조회
            EnhancedOrderBookDTO enhancedData = enhancedOrderBookService.getEnhancedOrderBook(ticker);

            if (enhancedData == null || 
                (enhancedData.getBids().isEmpty() && enhancedData.getAsks().isEmpty())) {
                // 빈 호가 데이터 반환
                log.warn("[ORDERBOOK-STOMP] ⚠️ No orderbook data found: ticker={}", ticker);
                return Map.of(
                        "type", "orderbook",
                        "ticker", ticker,
                        "data", Map.of(
                                "ticker", ticker,
                                "bids", new ArrayList<>(),
                                "asks", new ArrayList<>(),
                                "message", "No orderbook data available"
                        ),
                        "timestamp", System.currentTimeMillis()
                );
            }

            // 초기 데이터 반환
            return Map.of(
                    "type", "orderbook",
                    "ticker", ticker,
                    "data", enhancedData,
                    "timestamp", System.currentTimeMillis()
            );

        } catch (Exception e) {
            log.error("[ORDERBOOK-STOMP] ❌ Failed to send initial data: ticker={}", ticker, e);
            return Map.of(
                    "type", "error",
                    "ticker", ticker,
                    "message", "Failed to load orderbook data",
                    "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * 호가 업데이트 이벤트 리스너
     *
     * 이벤트 발생 시 Redis Pub/Sub으로 발행하여 모든 인스턴스가 수신
     */
    @EventListener
    public void handleOrderBookUpdateEvent(OrderBookUpdateEvent event) {
        try {
            String ticker = event.getTicker();
            OrderBookUpdateEvent.UpdateType updateType = event.getUpdateType();

            log.info("[ORDERBOOK-STOMP] 🎧 Event received: ticker={}, type={}", ticker, updateType);

            if (updateType == OrderBookUpdateEvent.UpdateType.ENHANCED) {
                log.info("[ORDERBOOK-STOMP] 🔄 Processing ENHANCED update: ticker={}", ticker);
                publishEnhancedOrderBook(ticker);
            } else {
                log.debug("[ORDERBOOK-STOMP] ⏭️ Skipping non-ENHANCED update: ticker={}, type={}", ticker, updateType);
            }

        } catch (Exception e) {
            log.error("[ORDERBOOK-STOMP] ❌ Failed to handle event: ticker={}", 
                    event.getTicker(), e);
            log.error("[ORDERBOOK-STOMP] Error details:", e);
        }
    }

    /**
     * 고도화된 호가 데이터를 Redis Pub/Sub으로 발행
     *
     * 채널: market:orderbook (ticker 정보는 메시지 내부에 포함)
     * RedisPubSubListener가 수신하여 /topic/orderbook/{ticker}로 전송
     *
     * ✅ 변경사항:
     * - StringRedisTemplate + JSON 문자열 → RedisTemplate + Map 객체 직접 전송
     * - 체결(Execution) 데이터와 동일한 방식으로 통일하여 직렬화/역직렬화 문제 해결
     */
    private void publishEnhancedOrderBook(String ticker) {
        try {
            // 고도화된 호가 데이터 조회
            EnhancedOrderBookDTO enhancedData = enhancedOrderBookService.getEnhancedOrderBook(ticker);

            if (enhancedData == null) {
                log.warn("[ORDERBOOK-STOMP] ⚠️ Enhanced data is null: ticker={}", ticker);
                return;
            }

            // 메시지 구성
            Map<String, Object> message = new HashMap<>();
            message.put("type", "orderbook-enhanced"); // ✅ 명확한 타입 지정
            message.put("ticker", ticker);
            message.put("data", enhancedData);
            message.put("timestamp", System.currentTimeMillis());

            // ✅ Map 객체를 그대로 전송 (RedisTemplate이 자동으로 직렬화)
            // JSON 문자열로 직렬화하지 않음 - 이중 직렬화 방지 및 EnhancedOrderBookDTO 구조 보존
            redisTemplate.convertAndSend(REDIS_CHANNEL, message);

            log.info("[ORDERBOOK-STOMP] 📤 Published: channel={}, ticker={}, bids={}, asks={}", 
                    REDIS_CHANNEL, ticker, 
                    enhancedData.getBids() != null ? enhancedData.getBids().size() : 0, 
                    enhancedData.getAsks() != null ? enhancedData.getAsks().size() : 0);

        } catch (Exception e) {
            log.error("[ORDERBOOK-STOMP] ❌ Failed to publish: ticker={}", ticker, e);
            log.error("[ORDERBOOK-STOMP] Error details:", e);
        }
    }
}
