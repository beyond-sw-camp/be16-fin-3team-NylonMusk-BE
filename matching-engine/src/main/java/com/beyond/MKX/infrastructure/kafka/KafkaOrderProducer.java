package com.beyond.MKX.infrastructure.kafka;

import com.beyond.MKX.infrastructure.kafka.event.ExecutionEvent;
import com.beyond.MKX.infrastructure.kafka.event.OrderStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 매칭 엔진 결과/상태/에러를 카프카로 발행하는 프로듀서.
 *
 * 표준화 원칙
 * - order-status 이벤트의 표시가격(price)은 "지정가 주문 입력값(=limitPrice)"로 고정한다.
 *   - 부분/완전 체결에서도 price는 vwap/last가 아니라 limitPrice를 사용한다.
 *   - 체결 관련 보조지표는 lastFillPrice, limitPrice 필드로만 제공한다.
 */
@Component("kafkaOrderProducer")
@RequiredArgsConstructor
@Slf4j
public class KafkaOrderProducer {

    private static final String EXECUTION_TOPIC    = "executions";
    private static final String ORDER_STATUS_TOPIC = "order-status";
    private static final String ERROR_TOPIC        = "order-errors";

    /** JSON 직렬화: value 타입을 Object로 두고 컨버터에 위임 */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ---------------------------------------------------------------------
    // 체결 이벤트 (부분/완전 체결 1건마다 발행)
    // ---------------------------------------------------------------------
    public void sendExecution(String marketOrderId, String ticker, String side,
                              String counterOrderId, BigDecimal qty, long price) {
        String execId = marketOrderId + "-" + counterOrderId + "-" + UUID.randomUUID(); // 멱등키(충분히 유일)
        ExecutionEvent evt = ExecutionEvent.builder()
                .execId(execId)
                .marketOrderId(marketOrderId)
                .counterOrderId(counterOrderId)
                .ticker(ticker)
                .side(side)
                .price(price)          // 개별 체결의 실제 체결가
                .quantity(qty)
                .timestamp(Instant.now().toEpochMilli())
                .build();

        // 파티션 키=티커(동일 종목 내 순서 보장 강화)
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

    // ---------------------------------------------------------------------
    // 상태 알림 (접수/대기/부분/완전/취소)
    // ---------------------------------------------------------------------
    /** 신규 지정가 접수(체결 전): price=지정가, remaining=초기 수량 */
    public void sendNewAccepted(String orderId, String ticker, String side, long price, BigDecimal qty) {
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

    /** 취소 성공 */
    public void sendCancelSuccess(String orderId) {
        OrderStatusEvent evt = OrderStatusEvent.builder()
                .orderId(orderId).status("CANCEL_OK")
                .timestamp(Instant.now().toEpochMilli()).build();
        sendOrderStatus(orderId, evt);        // 키=orderId (특정 주문 추적)
    }

    /** 시장가 대기(가드 범위 내 반대 호가 없음) */
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

    /**
     * 부분 체결 알림
     * - 표시가격(price)은 '지정가(limitPrice)'로 고정한다. (요구사항: 로그/컨슈머에서 주문 입력가 그대로 보이도록)
     * - 체결 관련 보조지표는 lastFillPrice/limitPrice로만 제공한다.
     */
    public void sendMarketPartial(String orderId, String ticker, String side,
                                  BigDecimal remaining,
                                  long vwap, long lastPrice, long limitPrice, BigDecimal filledQty) {
        OrderStatusEvent evt = OrderStatusEvent.builder()
                .orderId(orderId)
                .ticker(ticker)
                .side(side)
                .status("MARKET_PARTIAL")
                .remaining(remaining)
                .price(limitPrice)
                .lastFillPrice(lastPrice)
                .limitPrice(limitPrice)
                .filledQuantity(filledQty)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        sendOrderStatus(ticker != null ? ticker : orderId, evt);
    }

    /**
     * 완전 체결 알림
     * - 표시가격(price)은 '지정가(limitPrice)'로 고정한다.
     */
    public void sendMarketFilled(String orderId, String ticker, String side,
                                 long vwap, long lastPrice, long limitPrice, BigDecimal filledQty) {
        OrderStatusEvent evt = OrderStatusEvent.builder()
                .orderId(orderId)
                .ticker(ticker)
                .side(side)
                .status("MARKET_FILLED")
                .price(limitPrice)
                .lastFillPrice(lastPrice)
                .limitPrice(limitPrice)
                .filledQuantity(filledQty)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        sendOrderStatus(ticker != null ? ticker : orderId, evt);
    }

    // ---------------------------------------------------------------------
    // 에러(문자열 페이로드)
    // ---------------------------------------------------------------------
    public void sendError(String orderId, String reason) {
        String payload = String.format("Error on order %s: %s", orderId, reason);
        kafkaTemplate.send(ERROR_TOPIC, orderId, payload);
        log.error("[KAFKA] {}", payload);
    }

    // ---------------------------------------------------------------------
    // 내부 헬퍼: order-status 발행(공통 완료 콜백)
    // ---------------------------------------------------------------------
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
