package com.beyond.MKX.infrastructure.kafka.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final InboundOrderProcessor processor;
    private final ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(
            topics = "place-order",
            groupId = "matching-engine-inbound",
            containerFactory = "kafkaStringListenerFactory"
    )
    public void onInbound(
            String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY)   String key
    ) {
        log.info("주문 카프카 메시지 수신 : {} key 값 : {}", payload, key);
        try {
            InboundOrderMessage m = mapper.readValue(payload, InboundOrderMessage.class);
            processor.handleInbound(m);  // Redis 적재/매칭으로 연결
        } catch (Exception e) {
            log.error("[INBOUND] parsing failed. payload={}", payload, e);
        }
    }
}
