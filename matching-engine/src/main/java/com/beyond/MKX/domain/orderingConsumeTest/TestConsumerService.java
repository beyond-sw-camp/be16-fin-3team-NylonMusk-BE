package com.beyond.MKX.domain.orderingConsumeTest;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class TestConsumerService {

    @KafkaListener(
            topics = "place-order",
            groupId = "${spring.kafka.consumer.test-group-id1}",
            containerFactory = "kafkaListener"
    )
    public void consumer(
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            String message
    ) {
        System.out.println("주문 카프카 메시지 수신 : " + message + "key 값 : " + key);
    }

    @KafkaListener(
            topics = "place-order",
            groupId = "${spring.kafka.consumer.test-group-id2}",
            containerFactory = "kafkaListener"
    )
    public void consumer2(
            String message
    ) {
        System.out.println("주문 카프카 메시지 수신 : " + message);
    }


}
