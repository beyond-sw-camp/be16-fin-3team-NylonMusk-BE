package com.beyond.MKX.infrastructure.kafka.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * place-order 토픽 수신용 문자열 컨슈머.
 *
 * 역할
 * - 문자열 payload(JSON)를 수신 → ObjectMapper로 InboundOrderMessage 역직렬화
 * - 역직렬화된 메시지를 InboundOrderProcessor로 위임(도메인 이벤트 변환 및 매칭 처리)
 *
 * 비고
 * - containerFactory="kafkaStringListenerFactory": 문자열 수신에 최적화된 컨테이너 사용
 * - ObjectMapper는 구성 후 스레드 세이프하므로 필드로 재사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final InboundOrderProcessor processor;
    private final ObjectMapper mapper = new ObjectMapper(); // JSON 역직렬화기(공용 재사용)

    /**
     * place-order 토픽 리스너.
     * - 그룹: matching-engine-inbound (운영 소비 그룹과 동일하게 수신)
     * - 헤더(topic/key) 로깅으로 트레이싱 용이
     */
    @KafkaListener(
            topics = "ORDER_PLACED",
            groupId = "matching-engine-inbound",
            containerFactory = "kafkaStringListenerFactory"
    )
    public void onInbound(
            String payload,                                 // JSON 문자열 페이로드
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY)   String key
    ) {
        log.info("주문 카프카 메시지 수신 : {} key 값 : {}", payload, key);
        try {
            // JSON → DTO 변환(알 수 없는 필드는 DTO의 @JsonIgnoreProperties로 무시)
            InboundOrderMessage m = mapper.readValue(payload, InboundOrderMessage.class);
            // 매칭 엔진 플로우로 위임(매핑→처리)
            processor.handleInbound(m);
        } catch (Exception e) {
            // 역직렬화/처리 예외 로깅(필요 시 재시도/에러 토픽 연동을 상위에서 구성)
            log.error("[INBOUND] parsing failed. payload={}", payload, e);
        }
    }
}
