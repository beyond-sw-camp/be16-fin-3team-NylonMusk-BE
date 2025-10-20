package com.beyond.MKX.domain.orderingConsumeTest;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 주문 소비(consume) 테스트용 Kafka 리스너.
 *
 * 구성 요약
 * - 동일 토픽(place-order)을 서로 다른 groupId로 구독하여 독립 소비 시나리오 검증
 * - 컨테이너 팩토리: "kafkaListener" (KafkaConsumerConfig에서 등록)
 *
 * 참고
 * - 현재는 System.out 출력으로 단순 확인만 수행(운영 시 로그 프레임워크 사용 권장)
 */
@Component
public class TestConsumerService {

    /**
     * 테스트 컨슈머 #1
     * - 메시지 Key를 함께 수신하여 파티션 키/라우팅 검증에 활용
     *
     * @param key     Kafka 레코드 키(파티션 라우팅 기준)
     * @param message 문자열 페이로드(프로듀서에서 JSON 문자열 등)
     */
    @KafkaListener(
            topics = "ORDER_PLACED",
            groupId = "${spring.kafka.consumer.test-group-id1}",
            containerFactory = "kafkaListener"
    )
    public void consumer(
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            String message
    ) {
        System.out.println("TestConsService-주문 카프카 메시지 수신 : " + message + "\nkey 값 : " + key);
    }

    /**
     * 테스트 컨슈머 #2
     * - 동일 토픽을 다른 groupId로 구독하여 독립적인 소비 그룹 동작 확인
     *
     * @param message 문자열 페이로드
     */
    @KafkaListener(
            topics = "place-order",
            groupId = "${spring.kafka.consumer.test-group-id2}",
            containerFactory = "kafkaListener"
    )
    public void consumer2(
            String message
    ) {
        System.out.println("TestConsService-주문 카프카 메시지 수신 : " + message);
    }


}
