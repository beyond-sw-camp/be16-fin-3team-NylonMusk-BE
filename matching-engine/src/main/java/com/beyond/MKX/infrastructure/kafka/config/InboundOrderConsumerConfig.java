package com.beyond.MKX.infrastructure.kafka.config;

import com.beyond.MKX.infrastructure.kafka.event.InboundOrderMessage;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * 인바운드 주문(placing 서비스 → 매칭엔진) 수신 전용 Kafka Consumer 설정.
 *
 * 구성 요약
 * - ErrorHandlingDeserializer: 역직렬화 예외 발생 시 안전 처리(레코드 스킵/seek 가능)
 * - JsonDeserializer: value 타입을 InboundOrderMessage로 고정, TRUSTED_PACKAGES="*"
 * - ListenerContainerFactory: 단건 처리 기본값, 필요 시 동시성/에러 핸들러 확장
 *
 * 비고
 * - 브로커/그룹ID 등 공통 속성은 Spring Boot의 KafkaProperties(application 설정)에서 주입됩니다.
 */
@EnableKafka
@Configuration
public class InboundOrderConsumerConfig {

    /**
     * 인바운드 주문 메시지 컨슈머 팩토리.
     * - KEY: StringDeserializer
     * - VALUE: JsonDeserializer<InboundOrderMessage> (ErrorHandlingDeserializer로 감싸 안전화)
     * - 신뢰 패키지: "*" (멀티 모듈/패키지 이관 시 호환성 보장)
     */
    @Bean
    public ConsumerFactory<String, InboundOrderMessage> inboundOrderConsumerFactory(KafkaProperties props) {
        // Boot의 표준 consumer 설정을 기반으로 추가 옵션 덮어쓰기
        Map<String, Object> cfg = new HashMap<>(props.buildConsumerProperties(null));

        // 역직렬화 오류를 안전하게 처리하기 위해 ErrorHandlingDeserializer 사용
        cfg.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        cfg.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        // 실제 위임 역직렬화기(delegate) 지정
        cfg.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        cfg.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // Json 역직렬화 기본 타입 및 신뢰 패키지 설정
        cfg.put(JsonDeserializer.VALUE_DEFAULT_TYPE, InboundOrderMessage.class.getName());
        cfg.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        // 필요 시 타입 헤더 사용을 강제하려면 아래를 활성화 (프로듀서가 헤더를 넣는 경우)
        // cfg.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        return new DefaultKafkaConsumerFactory<>(cfg);
    }

    /**
     * 인바운드 주문 리스너 컨테이너 팩토리.
     * - @KafkaListener(containerFactory="inboundOrderListenerFactory")로 주입하여 사용
     * - 동시성(concurrency) 및 에러 핸들러는 운영 환경에 맞춰 추가 설정 가능
     */
    @Bean(name = "inboundOrderListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, InboundOrderMessage> inboundOrderListenerFactory(
            ConsumerFactory<String, InboundOrderMessage> cf) {
        ConcurrentKafkaListenerContainerFactory<String, InboundOrderMessage> f =
                new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(cf);
        // f.setConcurrency(3); // 필요 시 동시성 조정
        // f.setCommonErrorHandler(…); // 공통 에러 핸들러 적용 가능
        return f;
    }
}
