package com.beyond.MKX.domain.orderingConsumeTest;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 주문 소비(consume) 테스트용 Kafka Consumer 설정.
 *
 * 구성 요약
 * - bootstrap-servers: application 설정에서 주입
 * - 역직렬화기: Key/Value 모두 문자열(StringDeserializer) 사용
 * - 리스너 컨테이너 팩토리: 단일 레코드 처리 기본값으로 등록
 *
 * 주의
 * - 아래 consumerFactory는 <String, Object>로 선언되어 있으나 실제 역직렬화기는 문자열입니다.
 *   테스트 목적 상 동작에는 문제 없으나 일관성을 위해 <String, String> 변경을 고려할 수 있습니다.
 */
@Configuration
public class KafkaConsumerConfig {
    /** Kafka 브로커 연결 문자열 (예: "localhost:9092") */
    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaServer;

    /**
     * 테스트용 ConsumerFactory 생성.
     * - 문자열 페이로드 수신을 위해 Key/Value 역직렬화기를 String으로 지정
     * - 추가 옵션(그룹ID, 오프셋 리셋 정책 등)은 필요 시 확장
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory(){
        Map<String, Object> config = new HashMap<>();
        // 브로커 주소
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        // Key/Value 역직렬화기: 문자열 수신
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * 리스너 컨테이너 팩토리.
     * - @KafkaListener(…, containerFactory="kafkaListener")로 사용
     * - 테스트 시 단건 처리(배치 비활성) 기본값
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListener(){
        ConcurrentKafkaListenerContainerFactory<String, String> listener
                = new ConcurrentKafkaListenerContainerFactory<>();
        // 위의 ConsumerFactory 사용 (주의: 제네릭 불일치 가능성은 테스트 목적상 허용)
        listener.setConsumerFactory(consumerFactory());
        return listener;
    }
}
