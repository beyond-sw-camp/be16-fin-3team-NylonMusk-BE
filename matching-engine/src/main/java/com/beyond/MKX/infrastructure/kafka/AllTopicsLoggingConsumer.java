package com.beyond.MKX.infrastructure.kafka;

import com.beyond.MKX.infrastructure.kafka.event.ExecutionEvent;
import com.beyond.MKX.infrastructure.kafka.event.OrderStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AllTopicsLoggingConsumer {

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
        log.info("[KAFKA/ORDER-STATUS] topic={} p={} off={} orderId={} status={} ticker={} side={} remaining={} price={} ts={}",
                topic, partition, offset,
                payload.getOrderId(), payload.getStatus(), payload.getTicker(), payload.getSide(),
                payload.getRemaining(), payload.getPrice(), payload.getTimestamp());
    }

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
