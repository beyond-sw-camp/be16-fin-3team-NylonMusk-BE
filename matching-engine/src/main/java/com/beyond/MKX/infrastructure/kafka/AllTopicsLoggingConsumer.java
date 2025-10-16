package com.beyond.MKX.infrastructure.kafka;

import com.beyond.MKX.infrastructure.kafka.event.ExecutionEvent;
import com.beyond.MKX.infrastructure.kafka.event.OrderStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 실행/상태/에러 전용 카프카 로깅 컨슈머.
 *
 * 목적
 * - 운영 관측성을 위해 executions / order-status / order-errors 각 토픽의 수신 이벤트를 구조화 로깅
 * - ListenerContainerFactory를 타입별로 분리해 역직렬화 안전성 확보
 *
 * 주의
 * - 로깅 외의 비즈니스 로직은 포함하지 않음(사이드 이펙트 최소화)
 */
@Slf4j
@Component
public class AllTopicsLoggingConsumer {

    /**
     * 체결 이벤트 로깅 리스너.
     * - payload 타입: {@link ExecutionEvent}
     * - containerFactory: "kafkaExecutionListenerFactory" (JsonDeserializer<ExecutionEvent>)
     */
    @KafkaListener(
            topics = "executions",
            groupId = "matching-engine-logger",
            containerFactory = "kafkaExecutionListenerFactory"
    )
    public void onExecution(
            ExecutionEvent payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("[KAFKA/EXECUTIONS] topic={} p={} off={} tradeId={} orderId={} counterOrderId={} ticker={} side={} qty={} price={} ts={}",
                topic, partition, offset,
                payload.getExecId(), payload.getMarketOrderId(), payload.getCounterOrderId(),
                payload.getTicker(), payload.getSide(), payload.getQuantity(), payload.getPrice(), payload.getTimestamp());
    }

    /**
     * 주문 상태 이벤트 로깅 리스너.
     * - payload 타입: {@link OrderStatusEvent}
     * - containerFactory: "kafkaOrderStatusListenerFactory" (JsonDeserializer<OrderStatusEvent>)
     */
    @KafkaListener(
            topics = "order-status",
            groupId = "matching-engine-logger",
            containerFactory = "kafkaOrderStatusListenerFactory"
    )
    public void onOrderStatus(
            OrderStatusEvent payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("[KAFKA/ORDER-STATUS] topic={} p={} off={} orderId={} status={} ticker={} side={} remaining={} price={} notional={} ts={}",
                topic, partition, offset,
                payload.getOrderId(), payload.getStatus(), payload.getTicker(), payload.getSide(),
                payload.getRemaining(), payload.getPrice(), payload.getNotional(), payload.getTimestamp());
    }

    /**
     * 에러 토픽 문자열 로깅 리스너.
     * - payload 타입: String (단순 문자열 메시지)
     * - containerFactory: "kafkaStringListenerFactory"
     */
    @KafkaListener(
            topics = "order-errors",
            groupId = "matching-engine-logger",
            containerFactory = "kafkaStringListenerFactory"
    )
    public void onError(
            String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.error("[KAFKA/ERRORS] topic={} p={} off={} payload={}", topic, partition, offset, payload);
    }
}
