package com.beyond.MKX.domain.orderbook.stomp;

import com.beyond.MKX.domain.orderbook.dto.enhanced.EnhancedOrderBookDTO;
import com.beyond.MKX.domain.orderbook.event.OrderBookUpdateEvent;
import com.beyond.MKX.domain.orderbook.service.EnhancedOrderBookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Component;
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
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

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

            log.debug("[ORDERBOOK-STOMP] 🎧 Event received: ticker={}, type={}", ticker, updateType);

            if (updateType == OrderBookUpdateEvent.UpdateType.ENHANCED) {
                publishEnhancedOrderBook(ticker);
            }

        } catch (Exception e) {
            log.error("[ORDERBOOK-STOMP] ❌ Failed to handle event: ticker={}", 
                    event.getTicker(), e);
        }
    }

    /**
     * 고도화된 호가 데이터를 Redis Pub/Sub으로 발행
     *
     * 채널: market:orderbook (ticker 정보는 메시지 내부에 포함)
     * RedisPubSubListener가 수신하여 /topic/orderbook/{ticker}로 전송
     */
    private void publishEnhancedOrderBook(String ticker) {
        try {
            // 고도화된 호가 데이터 조회
            EnhancedOrderBookDTO enhancedData = enhancedOrderBookService.getEnhancedOrderBook(ticker);

            // 메시지 구성
            Map<String, Object> message = new HashMap<>();
            message.put("type", "orderbook");
            message.put("ticker", ticker);
            message.put("data", enhancedData);
            message.put("timestamp", System.currentTimeMillis());

            // JSON 직렬화
            String messageJson = objectMapper.writeValueAsString(message);

            // Redis Pub/Sub 발행
            redisTemplate.convertAndSend(REDIS_CHANNEL, messageJson);

            log.debug("[ORDERBOOK-STOMP] 📤 Published: channel={}, ticker={}, bids={}, asks={}", 
                    REDIS_CHANNEL, ticker, 
                    enhancedData.getBids().size(), 
                    enhancedData.getAsks().size());

        } catch (Exception e) {
            log.error("[ORDERBOOK-STOMP] ❌ Failed to publish: ticker={}", ticker, e);
        }
    }
}
