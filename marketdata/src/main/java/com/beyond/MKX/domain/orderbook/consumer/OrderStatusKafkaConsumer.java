package com.beyond.MKX.domain.orderbook.consumer;

import com.beyond.MKX.domain.orderbook.dto.OrderStatusEventDTO;
import com.beyond.MKX.domain.orderbook.service.OrderBookService;
import com.beyond.MKX.domain.ranking.dto.UpdateRedisMarketRank;
import com.beyond.MKX.domain.ranking.service.MarketRankWriterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 주문 상태 이벤트 Kafka Consumer
 * 
 * ✅ 변경사항:
 * - orderbook 업데이트 로직 제거 (matching-engine이 직접 관리)
 * - 거래대금 통계 업데이트만 유지
 * - WebSocket 업데이트 이벤트만 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusKafkaConsumer {

    private final OrderBookService orderBookService;
    private final MarketRankWriterService marketRankWriterService;

    /**
     * order-status 토픽으로부터 주문 상태 이벤트 수신
     */
    @KafkaListener(
        topics = "${kafka.topics.order-status}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "orderStatusKafkaListenerContainerFactory"
    )
    public void consumeOrderStatus(
            @Payload OrderStatusEventDTO orderStatus,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        try {
            log.debug("[KAFKA/ORDER-STATUS] Received: orderId={}, status={}, ticker={}, notional={}",
                    orderStatus.getOrderId(), orderStatus.getStatus(),
                    orderStatus.getTicker(), orderStatus.getNotional());

            String status = orderStatus.getStatus();
            String ticker = orderStatus.getTicker();

            // ✅ orderbook 업데이트 로직 제거 - matching-engine이 직접 관리
            // 호가 변경 시나리오:
            // 1. NEW_ACCEPTED: 새 주문이 호가에 추가됨 → 호가 업데이트 필요
            // 2. MARKET_PARTIAL/MARKET_FILLED: 체결 발생 → 호가 업데이트 + 거래대금 통계 필요
            // 3. CANCELLED: 주문 취소 → 호가 업데이트 필요
            
            boolean shouldUpdateOrderBook = false;
            boolean shouldUpdateTurnover = false;
            
            if ("NEW_ACCEPTED".equals(status) || "LIMIT_ACCEPTED".equals(status)) {
                // 새 주문이 호가에 추가됨
                shouldUpdateOrderBook = true;
                log.debug("[ORDER-STATUS] New order accepted - orderbook update required: ticker={}, status={}", 
                        ticker, status);
            } else if ("MARKET_PARTIAL".equals(status) || "MARKET_FILLED".equals(status)) {
                // 체결 발생 - 호가 업데이트 + 거래대금 통계
                shouldUpdateOrderBook = true;
                shouldUpdateTurnover = true;
                log.debug("[ORDER-STATUS] Execution occurred - orderbook & turnover update required: ticker={}, status={}", 
                        ticker, status);
            } else if ("CANCELLED".equals(status)) {
                // 주문 취소 - 호가 업데이트 필요
                shouldUpdateOrderBook = true;
                log.debug("[ORDER-STATUS] Order cancelled - orderbook update required: ticker={}, status={}", 
                        ticker, status);
            }
            
            // 랭킹 통계 업데이트 (체결 시에만)
            if (shouldUpdateTurnover) {
                LocalDate tradingDate = translateTradingDate(orderStatus);
                marketRankWriterService.updateVolumeAndValueRank(
                        UpdateRedisMarketRank.builder()
                                .ticker(ticker)  // ✅ ticker 추가
                                .volume(orderStatus.getFilledQuantity())
                                .tradeValue(orderStatus.getNotional())
                                .tradingDate(tradingDate)
                                .build()
                );
                log.debug("[ORDER-STATUS] Updated turnover stats: ticker={}, notional={}", 
                        ticker, orderStatus.getNotional());
            }
            
            // 호가 업데이트 이벤트 발행 (주문 추가/체결/취소 모두)
            if (shouldUpdateOrderBook) {
                // ✅ 호가 업데이트 이벤트 직접 발행 (ExecutionEventDTO 없이)
                orderBookService.triggerOrderBookUpdate(ticker);
                log.info("[ORDER-STATUS] ✅ Triggered orderbook update: ticker={}, status={}", ticker, status);
            }

            // 수동 커밋
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("[KAFKA/ORDER-STATUS] ❌ Failed to process: {}", orderStatus, e);
            // 에러 발생 시에도 커밋하여 무한 재처리 방지
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        }
    }

    private static LocalDate translateTradingDate(OrderStatusEventDTO orderStatus) {
        long timestamp = orderStatus.getTimestamp();
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDate();
    }
}
