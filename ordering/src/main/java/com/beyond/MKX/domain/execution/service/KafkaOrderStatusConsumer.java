package com.beyond.MKX.domain.execution.service;

import com.beyond.MKX.common.kafka.event.ExecutionEvent;
import com.beyond.MKX.common.kafka.event.OrderStatusEvent;
import com.beyond.MKX.domain.order.entity.Side;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaOrderStatusConsumer {


    @KafkaListener(
            topics = "order-status",
            groupId = "${spring.kafka.consumer.order-status-group-id}",
            containerFactory = "kafkaOrderStatusListenerFactory"
    )
    public void processOrderStatus(
            @Payload OrderStatusEvent orderStatusEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment ack
    ) {
        try {
            System.out.println("=========== OrderStatus 이벤트 컨슘 시작 ===========");
            System.out.println("orderStatusEvent = " + orderStatusEvent);
            System.out.println("partition = " + partition + "  offset = " + offset);







            ack.acknowledge(); // 수동 커밋
            System.out.println("=========== OrderStatus 이벤트 컨슘 종료 및 커밋 ===========");
        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage());
            throw e;
        }

    }
}
