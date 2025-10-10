package com.beyond.MKX.infrastructure.kafka;

import com.beyond.MKX.infrastructure.kafka.event.ExecutionEvent;
import com.beyond.MKX.infrastructure.kafka.event.OrderStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component("kafkaOrderProducer")
@RequiredArgsConstructor
@Slf4j
public class KafkaOrderProducer {

    private static final String EXECUTION_TOPIC    = "executions";
    private static final String ORDER_STATUS_TOPIC = "order-status";
    private static final String ERROR_TOPIC        = "order-errors";

    // JSON 직렬화용: value를 Object로
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // -------- 체결 이벤트 (부분/완전 체결 1건마다) --------
    public void sendExecution(String marketOrderId, String ticker, String side,
                              String counterOrderId, double qty, double price) {
        String execId = marketOrderId + "-" + counterOrderId + "-" + UUID.randomUUID(); // 멱등키
        ExecutionEvent evt = ExecutionEvent.builder()
                .execId(execId)
                .marketOrderId(marketOrderId)
                .counterOrderId(counterOrderId)
                .ticker(ticker)
                .side(side)
                .price(price)
                .quantity(qty)
                .timestamp(Instant.now().toEpochMilli())
                .build();

        // 순서 일관성을 위해 파티션 키는 종목으로
        kafkaTemplate.send(EXECUTION_TOPIC, ticker, evt).whenComplete((md, ex) -> {
            if (ex != null) {
                log.error("[KAFKA] executions send failed key={} evt={}", ticker, evt, ex);
            } else {
                log.debug("[KAFKA] executions sent topic={} partition={} offset={}",
                        md.getRecordMetadata().topic(),
                        md.getRecordMetadata().partition(),
                        md.getRecordMetadata().offset());
            }
        });
    }

    // -------- 상태 알림 --------
    public void sendNewAccepted(String orderId, String ticker, String side, double price, double qty) {
        OrderStatusEvent evt = OrderStatusEvent.builder()
                .orderId(orderId)
                .ticker(ticker)
                .side(side)
                .status("NEW_ACCEPTED")
                .price(price)     // LIMIT 주문의 지정가
                .remaining(qty)   // 초기 잔량 = 접수 수량
                .timestamp(Instant.now().toEpochMilli())
                .build();
        sendOrderStatus(ticker != null ? ticker : orderId, evt);
    }

    public void sendCancelSuccess(String orderId) {
        OrderStatusEvent evt = OrderStatusEvent.builder()
                .orderId(orderId).status("CANCEL_OK")
                .timestamp(Instant.now().toEpochMilli()).build();
        sendOrderStatus(orderId, evt);
    }

    // 변경된 메서드들만 발췌
    public void sendWaiting(String marketOrderId, String ticker, String side) {
        OrderStatusEvent evt = OrderStatusEvent.builder()
                .orderId(marketOrderId)
                .status("WAITING")
                .ticker(ticker)
                .side(side)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        sendOrderStatus(ticker != null ? ticker : marketOrderId, evt);
    }

    public void sendMarketPartial(String orderId, String ticker, String side,
                                  double remaining,
                                  double vwap, double lastPrice, double limitPrice, double filledQty) {
        OrderStatusEvent evt = OrderStatusEvent.builder()
                .orderId(orderId)
                .ticker(ticker)
                .side(side)
                .status("MARKET_PARTIAL")
                .remaining(remaining)
                .price(vwap)
                .lastFillPrice(lastPrice)
                .limitPrice(limitPrice)
                .filledQuantity(filledQty)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        sendOrderStatus(ticker != null ? ticker : orderId, evt);
    }

    public void sendMarketFilled(String orderId, String ticker, String side,
                                 double vwap, double lastPrice, double limitPrice, double filledQty) {
        OrderStatusEvent evt = OrderStatusEvent.builder()
                .orderId(orderId)
                .ticker(ticker)
                .side(side)
                .status("MARKET_FILLED")
                .price(vwap)
                .lastFillPrice(lastPrice)
                .limitPrice(limitPrice)
                .filledQuantity(filledQty)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        sendOrderStatus(ticker != null ? ticker : orderId, evt);
    }

    // -------- 에러 --------
    public void sendError(String orderId, String reason) {
        String payload = String.format("Error on order %s: %s", orderId, reason);
        kafkaTemplate.send(ERROR_TOPIC, orderId, payload);
        log.error("[KAFKA] {}", payload);
    }

    private void sendOrderStatus(String key, Object payload) {
        kafkaTemplate.send(ORDER_STATUS_TOPIC, key, payload)
                .whenComplete((md, ex) -> {
                    if (ex != null) {
                        log.error("[KAFKA] order-status send failed key={} payload={}", key, payload, ex);
                    } else {
                        log.debug("[KAFKA] order-status sent topic={} partition={} offset={}",
                                md.getRecordMetadata().topic(),
                                md.getRecordMetadata().partition(),
                                md.getRecordMetadata().offset());
                    }
                });
    }
}
