package com.beyond.MKX.domain.execution.stomp;

import com.beyond.MKX.domain.execution.dto.ExecutionEventDTO;
import com.beyond.MKX.domain.execution.service.ExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

/**
 * 체결 STOMP 컨트롤러
 *
 * 실시간 체결 데이터를 Redis Pub/Sub으로 발행
 * 채널: market:trades
 * STOMP 구독자들은 RedisPubSubListener를 통해 /topic/trades/{ticker}로 수신
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ExecutionStompController {

    private final ExecutionService executionService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis Pub/Sub 채널명 (간소화)
    private static final String REDIS_CHANNEL = "market:trades";

    /**
     * 초기 구독 시 최근 체결 데이터 즉시 전송
     *
     * 클라이언트가 /topic/trades/{ticker}를 구독하면 즉시 최근 체결 데이터를 반환
     * 이후 업데이트는 Redis Pub/Sub을 통해 전송됨
     *
     * @param ticker 종목 코드
     * @return 체결 데이터 메시지
     */
    @SubscribeMapping("/topic/trades/{ticker}")
    public Map<String, Object> onSubscribe(@DestinationVariable String ticker) {
        log.info("[EXECUTION-STOMP] 🔔 New subscription: ticker={}", ticker);

        try {
            // 최근 체결 데이터 조회 (10건)
            List<ExecutionEventDTO> recentExecutions = executionService.getRecentExecutions(ticker, 10);

            if (recentExecutions == null || recentExecutions.isEmpty()) {
                // 빈 데이터 반환
                log.warn("[EXECUTION-STOMP] ⚠️ No execution data found: ticker={}", ticker);
                return Map.of(
                        "type", "trades",
                        "ticker", ticker,
                        "data", new ArrayList<>(),
                        "message", "No trading history available",
                        "timestamp", System.currentTimeMillis()
                );
            }

            // 초기 데이터 반환
            return Map.of(
                    "type", "trades",
                    "ticker", ticker,
                    "data", recentExecutions,
                    "timestamp", System.currentTimeMillis()
            );

        } catch (Exception e) {
            log.error("[EXECUTION-STOMP] ❌ Failed to send initial data: ticker={}", ticker, e);
            return Map.of(
                    "type", "error",
                    "ticker", ticker,
                    "message", "Failed to load execution data",
                    "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * 실시간 체결 데이터를 Redis Pub/Sub으로 발행
     *
     * 채널: market:trades (ticker 정보는 메시지 내부에 포함)
     * RedisPubSubListener가 수신하여 /topic/trades/{ticker}로 전송
     *
     * @param execution 체결 데이터
     */
    public void publishExecution(ExecutionEventDTO execution) {
        try {
            String ticker = execution.getTicker();

            // 메시지 구성
            Map<String, Object> message = new HashMap<>();
            message.put("type", "execution");
            message.put("ticker", ticker);
            message.put("data", Map.of(
                    "execId", execution.getExecId(),
                    "ticker", execution.getTicker(),
                    "side", execution.getSide(),
                    "price", execution.getPrice(),
                    "quantity", execution.getQuantity(),
                    "timestamp", execution.getTimestamp()
            ));
            message.put("timestamp", System.currentTimeMillis());

            // JSON 직렬화
            String messageJson = objectMapper.writeValueAsString(message);

            // Redis Pub/Sub 발행
            redisTemplate.convertAndSend(REDIS_CHANNEL, messageJson);

            log.debug("[EXECUTION-STOMP] 📤 Published: channel={}, ticker={}, price={}, qty={}", 
                    REDIS_CHANNEL, ticker, execution.getPrice(), execution.getQuantity());

        } catch (Exception e) {
            log.error("[EXECUTION-STOMP] ❌ Failed to publish: ticker={}", 
                    execution.getTicker(), e);
        }
    }
}
