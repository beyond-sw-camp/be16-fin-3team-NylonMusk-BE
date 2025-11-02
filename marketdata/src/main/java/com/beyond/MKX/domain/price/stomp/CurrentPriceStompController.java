package com.beyond.MKX.domain.price.stomp;

import com.beyond.MKX.domain.price.entity.CurrentPrice;
import com.beyond.MKX.domain.price.service.CurrentPriceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

/**
 * 현재가 STOMP 컨트롤러
 *
 * 실시간 현재가 데이터를 Redis Pub/Sub으로 발행
 * 채널: market:price
 * STOMP 구독자들은 RedisPubSubListener를 통해 /topic/price/{ticker}로 수신
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CurrentPriceStompController {

    private final CurrentPriceService currentPriceService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis Pub/Sub 채널명 (간소화)
    private static final String REDIS_CHANNEL = "market:price";

    /**
     * 초기 구독 시 현재가 데이터 즉시 전송
     *
     * 클라이언트가 /topic/price/{ticker}를 구독하면 즉시 현재가 데이터를 반환
     * 이후 업데이트는 Redis Pub/Sub을 통해 전송됨
     *
     * @param ticker 종목 코드
     * @return 현재가 데이터 메시지
     */
    @SubscribeMapping("/topic/price/{ticker}")
    public Map<String, Object> onSubscribe(@DestinationVariable String ticker) {
        log.info("[PRICE-STOMP] 🔔 New subscription: ticker={}", ticker);

        try {
            // Redis에서 현재가 조회
            CurrentPrice currentPrice = currentPriceService.getCurrentPrice(ticker);

            if (currentPrice == null) {
                // 빈 데이터 반환
                log.warn("[PRICE-STOMP] ⚠️ No price data found: ticker={}", ticker);
                return Map.of(
                        "type", "price",
                        "ticker", ticker,
                        "data", Map.of(
                                "ticker", ticker,
                                "price", 0,
                                "message", "No trading data available"
                        ),
                        "timestamp", System.currentTimeMillis()
                );
            }

            // 초기 데이터 반환
            return Map.of(
                    "type", "price",
                    "ticker", ticker,
                    "data", Map.of(
                            "ticker", currentPrice.getTicker(),
                            "price", currentPrice.getPrice(),
                            "change", currentPrice.getChange(),
                            "changePercent", currentPrice.getChangeRate(),
                            "volume", currentPrice.getVolume(),
                            "high", currentPrice.getHigh(),
                            "low", currentPrice.getLow(),
                            "open", currentPrice.getOpen(),
                            "previousClose", currentPrice.getPrevClose(),
                            "timestamp", currentPrice.getTimestamp()
                    ),
                    "timestamp", System.currentTimeMillis()
            );

        } catch (Exception e) {
            log.error("[PRICE-STOMP] ❌ Failed to send initial data: ticker={}", ticker, e);
            return Map.of(
                    "type", "error",
                    "ticker", ticker,
                    "message", "Failed to load price data",
                    "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * 현재가 데이터를 Redis Pub/Sub으로 발행
     *
     * 채널: market:price (ticker 정보는 메시지 내부에 포함)
     * RedisPubSubListener가 수신하여 /topic/price/{ticker}로 전송
     *
     * @param currentPrice 현재가 데이터
     */
    public void publishCurrentPrice(CurrentPrice currentPrice) {
        try {
            String ticker = currentPrice.getTicker();

            // 메시지 구성
            Map<String, Object> message = new HashMap<>();
            message.put("type", "price");
            message.put("ticker", ticker);
            message.put("data", Map.of(
                    "ticker", currentPrice.getTicker(),
                    "price", currentPrice.getPrice(),
                    "change", currentPrice.getChange(),
                    "changePercent", currentPrice.getChangeRate(),
                    "volume", currentPrice.getVolume(),
                    "high", currentPrice.getHigh(),
                    "low", currentPrice.getLow(),
                    "open", currentPrice.getOpen(),
                    "previousClose", currentPrice.getPrevClose(),
                    "timestamp", currentPrice.getTimestamp()
            ));
            message.put("timestamp", System.currentTimeMillis());

            // JSON 직렬화
            String messageJson = objectMapper.writeValueAsString(message);

            // Redis Pub/Sub 발행
            redisTemplate.convertAndSend(REDIS_CHANNEL, messageJson);

            log.debug("[PRICE-STOMP] 📤 Published: channel={}, ticker={}, price={}", 
                    REDIS_CHANNEL, ticker, currentPrice.getPrice());

        } catch (Exception e) {
            log.error("[PRICE-STOMP] ❌ Failed to publish: ticker={}", 
                    currentPrice.getTicker(), e);
        }
    }
}
