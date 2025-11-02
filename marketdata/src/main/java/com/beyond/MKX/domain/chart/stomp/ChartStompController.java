package com.beyond.MKX.domain.chart.stomp;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.chart.service.ChartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 차트 STOMP 컨트롤러
 *
 * 실시간 캔들 차트 데이터를 Redis Pub/Sub으로 발행
 * 채널: market:chart
 * STOMP 구독자들은 RedisPubSubListener를 통해 /topic/chart/{ticker}로 수신
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChartStompController {

    private final ChartService chartService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis Pub/Sub 채널명 (간소화)
    private static final String REDIS_CHANNEL = "market:chart";

    /**
     * 초기 구독 시 최근 캠들 데이터 즉시 전송
     *
     * 클라이언트가 /topic/chart/{ticker}를 구독하면 즉시 최근 캠들 데이터를 반환
     * 이후 업데이트는 Redis Pub/Sub을 통해 전송됨
     *
     * @param ticker 종목 코드
     * @return 차트 데이터 메시지
     */
    @SubscribeMapping("/topic/chart/{ticker}")
    public Map<String, Object> onSubscribe(@DestinationVariable String ticker) {
        log.info("[CHART-STOMP] 🔔 New subscription: ticker={}", ticker);

        try {
            // 최근 캠들 데이터 조회 (기본 1분봉, 100건)
            List<Candle> recentCandles = chartService.getRecentCandles(ticker, "1m", 100);

            if (recentCandles == null || recentCandles.isEmpty()) {
                // 빈 데이터 반환
                log.warn("[CHART-STOMP] ⚠️ No chart data found: ticker={}", ticker);
                return Map.of(
                        "type", "chart",
                        "ticker", ticker,
                        "data", new ArrayList<>(),
                        "message", "No chart data available",
                        "timestamp", System.currentTimeMillis()
                );
            }

            // 초기 데이터 반환
            return Map.of(
                    "type", "chart",
                    "ticker", ticker,
                    "data", recentCandles,
                    "timestamp", System.currentTimeMillis()
            );

        } catch (Exception e) {
            log.error("[CHART-STOMP] ❌ Failed to send initial data: ticker={}", ticker, e);
            return Map.of(
                    "type", "error",
                    "ticker", ticker,
                    "message", "Failed to load chart data",
                    "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * 캔들 데이터를 Redis Pub/Sub으로 발행
     *
     * 채널: market:chart (ticker 정보는 메시지 내부에 포함)
     * RedisPubSubListener가 수신하여 /topic/chart/{ticker}로 전송
     *
     * @param candle 캔들 데이터
     */
    public void publishCandle(Candle candle) {
        try {
            String ticker = candle.getTicker();

            // 메시지 구성
            Map<String, Object> message = new HashMap<>();
            message.put("type", "chart");
            message.put("ticker", ticker);
            message.put("data", Map.of(
                    "ticker", candle.getTicker(),
                    "interval", candle.getInterval(),
                    "time", candle.getTime(),
                    "open", candle.getOpen(),
                    "high", candle.getHigh(),
                    "low", candle.getLow(),
                    "close", candle.getClose(),
                    "volume", candle.getVolume()
            ));
            message.put("timestamp", System.currentTimeMillis());

            // JSON 직렬화
            String messageJson = objectMapper.writeValueAsString(message);

            // Redis Pub/Sub 발행
            redisTemplate.convertAndSend(REDIS_CHANNEL, messageJson);

            log.debug("[CHART-STOMP] 📤 Published: channel={}, ticker={}, interval={}, close={}",
                    REDIS_CHANNEL, ticker, candle.getInterval(), candle.getClose());

        } catch (Exception e) {
            log.error("[CHART-STOMP] ❌ Failed to publish: ticker={}", 
                    candle.getTicker(), e);
        }
    }
}
