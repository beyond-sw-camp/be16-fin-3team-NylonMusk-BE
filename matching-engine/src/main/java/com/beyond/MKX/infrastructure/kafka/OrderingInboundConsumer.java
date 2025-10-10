package com.beyond.MKX.infrastructure.kafka;

import com.beyond.MKX.infrastructure.kafka.event.InboundOrderMessage;
import com.beyond.MKX.infrastructure.kafka.event.InboundOrderProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderingInboundConsumer {

    private final InboundOrderProcessor processor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(
            topics = {"place-order"},
            groupId = "matching-engine-inbound"
    )
    public void onInbound(String payload,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.RECEIVED_KEY)   String key) {
        log.info("[INBOUND] topic={} key={} value={}", topic, key, payload);
        try {
            InboundOrderMessage m = objectMapper.readValue(payload, InboundOrderMessage.class);
            processor.handleInbound(m);
        } catch (Exception e) {
            log.error("[INBOUND] parse/handle failed. payload={}", payload, e);
        }
    }
}
