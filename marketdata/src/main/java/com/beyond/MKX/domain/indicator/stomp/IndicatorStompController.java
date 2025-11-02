package com.beyond.MKX.domain.indicator.stomp;

import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 보조지표 STOMP 컨트롤러
 *
 * 실시간 보조지표 데이터를 Redis Pub/Sub으로 발행
 * 채널: market:indicator
 * STOMP 구독자들은 RedisPubSubListener를 통해 /topic/indicator/{ticker}로 수신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorStompController {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis Pub/Sub 채널명 (간소화)
    private static final String REDIS_CHANNEL = "market:indicator";

    /**
     * 보조지표 데이터를 Redis Pub/Sub으로 발행
     *
     * 채널: market:indicator (ticker 정보는 메시지 내부에 포함)
     * RedisPubSubListener가 수신하여 /topic/indicator/{ticker}로 전송
     *
     * @param ticker 종목코드
     * @param indicatorResult 보조지표 결과
     */
    public void publishIndicator(String ticker, IndicatorResultDTO indicatorResult) {
        try {
            // 메시지 구성
            Map<String, Object> message = new HashMap<>();
            message.put("type", "indicator");
            message.put("ticker", ticker);
            message.put("data", indicatorResult);
            message.put("timestamp", System.currentTimeMillis());

            // JSON 직렬화
            String messageJson = objectMapper.writeValueAsString(message);

            // Redis Pub/Sub 발행
            redisTemplate.convertAndSend(REDIS_CHANNEL, messageJson);

            log.debug("[INDICATOR-STOMP] 📤 Published: channel={}, ticker={}, indicator={}", 
                    REDIS_CHANNEL, ticker, indicatorResult.getIndicatorType());

        } catch (Exception e) {
            log.error("[INDICATOR-STOMP] ❌ Failed to publish: ticker={}", ticker, e);
        }
    }
}
