package com.beyond.MKX.domain.orderbook.consumer;

import com.beyond.MKX.domain.orderbook.dto.OrderStatusEventDTO;
import com.beyond.MKX.domain.orderbook.service.OrderBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 주문 상태 이벤트 Kafka Consumer
 * 
 * matching-engine에서 발송하는 주문 상태 변화 이벤트를 수신하여 호가 업데이트
 * 
 * 주문 상태별 처리:
 * - NEW_ACCEPTED: 호가에 추가 (최초 1회만)
 * - WAITING: 이미 추가되었으므로 무시
 * - MARKET_PARTIAL: 부분 체결 시 남은 수량으로 업데이트
 * - MARKET_FILLED: 완전 체결 시 호가에서 제거
 * - CANCEL_OK: 취소 시 호가에서 제거
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusKafkaConsumer {

    private final OrderBookService orderBookService;
    
    // 이미 처리된 주문 ID를 추적 (중복 방지)
    private final Set<String> processedOrders = ConcurrentHashMap.newKeySet();

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
            log.info("[KAFKA/ORDER-STATUS] 📨 Received from topic={}: orderId={}, status={}, ticker={}, side={}, price={}, remaining={}",
                    topic,
                    orderStatus.getOrderId(), orderStatus.getStatus(),
                    orderStatus.getTicker(), orderStatus.getSide(),
                    orderStatus.getPrice(), orderStatus.getRemaining());

            // 주문 상태에 따라 처리
            String status = orderStatus.getStatus();
            String orderId = orderStatus.getOrderId();
            
            if ("NEW_ACCEPTED".equals(status)) {
                // 새로운 주문 → 호가에 추가 (최초 1회만)
                if (!processedOrders.contains(orderId)) {
                    orderBookService.addOrderToBook(orderStatus);
                    processedOrders.add(orderId);
                    log.info("[ORDER-STATUS] ✅ Added to orderbook: orderId={}, ticker={}, side={}, price={}, qty={}", 
                            orderId, orderStatus.getTicker(), orderStatus.getSide(), 
                            orderStatus.getPrice(), orderStatus.getRemaining());
                } else {
                    log.warn("[ORDER-STATUS] ⚠️ Duplicate NEW_ACCEPTED ignored: orderId={}", orderId);
                }
                
            } else if ("WAITING".equals(status)) {
                // WAITING은 이미 NEW_ACCEPTED에서 추가했으므로 무시
                log.debug("[ORDER-STATUS] Order already in orderbook (WAITING): orderId={}", orderId);
                
            } else if ("MARKET_PARTIAL".equals(status)) {
                // 부분 체결 → 남은 수량으로 업데이트
                orderBookService.updateOrderQuantity(orderStatus);
                log.info("[ORDER-STATUS] ✅ Updated quantity (PARTIAL): orderId={}, remaining={}", 
                        orderId, orderStatus.getRemaining());
                
            } else if ("MARKET_FILLED".equals(status)) {
                // 완전 체결 → 호가에서 제거
                orderBookService.removeOrderFromBook(orderStatus);
                processedOrders.remove(orderId);
                log.info("[ORDER-STATUS] ✅ Removed from orderbook (FILLED): orderId={}, ticker={}, side={}, price={}", 
                        orderId, orderStatus.getTicker(), orderStatus.getSide(), orderStatus.getPrice());
                
            } else if ("CANCEL_OK".equals(status)) {
                // 주문 취소 → 호가에서 제거
                orderBookService.removeOrderFromBook(orderStatus);
                processedOrders.remove(orderId);
                log.info("[ORDER-STATUS] ✅ Removed from orderbook (CANCELLED): orderId={}", orderId);
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
}
