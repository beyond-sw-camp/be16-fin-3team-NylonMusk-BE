package com.beyond.MKX.infrastructure.kafka.config;

import com.beyond.MKX.infrastructure.kafka.event.ExecutionEvent;
import com.beyond.MKX.infrastructure.kafka.event.OrderStatusEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * 실행/주문상태/문자열 수신을 위한 Kafka Listener 공통 설정.
 *
 * 구성 개요
 * - ErrorHandlingDeserializer 로 역직렬화 예외를 안전 처리(재시도·스킵)
 * - JsonDeserializer 기본 타입 고정(헤더 부재 시에도 안전) + TRUSTED_PACKAGES="*"
 * - 공통 DefaultErrorHandler(지수 백오프) 적용
 * - 이벤트 타입별 ConsumerFactory/ListenerFactory 분리
 */
@EnableKafka
@Configuration
public class KafkaListenerConfig {

    /**
     * 공통 Consumer 설정 빌더(Json 기반).
     * - KEY/VAL: ErrorHandlingDeserializer 사용
     * - 실제 delegate: KEY=StringDeserializer, VAL=JsonDeserializer
     * - 타입 헤더 사용 허용(USE_TYPE_INFO_HEADERS=true)
     */
    private Map<String, Object> baseConsumerConfigs(KafkaProperties properties) {
        Map<String, Object> configs = new HashMap<>(properties.buildConsumerProperties(null));
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        // 실제 delegate 지정
        configs.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());
        configs.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        // JSON 공통 설정
        configs.put(JsonDeserializer.TRUSTED_PACKAGES, "*");           // 패키지 신뢰(모듈 이동 대응)
        configs.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);     // 프로듀서 타입 헤더 사용 시 호환
        return configs;
    }

    // ----------------------------------------------------------------------
    // executions 전용 ConsumerFactory (ExecutionEvent)
    //  - VALUE_DEFAULT_TYPE 고정: 헤더 부재/문자열 발행에도 안전
    // ----------------------------------------------------------------------
    @Bean
    public ConsumerFactory<String, ExecutionEvent> executionConsumerFactory(KafkaProperties properties) {
        Map<String, Object> configs = baseConsumerConfigs(properties);
        // 기본 타입 고정 (헤더가 없거나 문자열일 때도 안전)
        configs.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ExecutionEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    // ----------------------------------------------------------------------
    // order-status 전용 ConsumerFactory (OrderStatusEvent)
    // ----------------------------------------------------------------------
    @Bean
    public ConsumerFactory<String, OrderStatusEvent> orderStatusConsumerFactory(KafkaProperties properties) {
        Map<String, Object> configs = baseConsumerConfigs(properties);
        configs.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderStatusEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    // ----------------------------------------------------------------------
    // 문자열 전용 ConsumerFactory (에러/보조 토픽 등)
    //  - 단순 문자열 수신이므로 ErrorHandlingDeserializer 생략
    // ----------------------------------------------------------------------
    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory(KafkaProperties properties) {
        Map<String, Object> configs = new HashMap<>(properties.buildConsumerProperties(null));
        // 문자열은 굳이 ErrorHandlingDeserializer 필요 없음 (원하면 동일 패턴 적용 가능)
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    /**
     * 공통 에러 핸들러.
     * - 역직렬화/처리 중 에러 시 지수 백오프 재시도 후 레코드 스킵
     * - 초기 100ms, 배수 2.0, 최대 5회, 최대 간격 2s
     */

    // 위치 이유: Kafka 전용 설정 결합·팩토리별 튜닝·모듈 경계 유지를 위해 common/exception이 아닌 이 설정 내부에 둠.
    private DefaultErrorHandler commonErrorHandler() {
        // 초기 100ms, 배수 2.0, 최대 5회 재시도 후 레코드 스킵
        ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(5);
        backoff.setInitialInterval(100);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(2_000);
        return new DefaultErrorHandler(backoff);
    }

    // ----------------------------------------------------------------------
    // ListenerContainerFactory 들
    //  - 배치 리스닝 비활성(단건 처리 기본)
    //  - 공통 에러 핸들러 적용
    // ----------------------------------------------------------------------
    @Bean(name = "kafkaExecutionListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, ExecutionEvent> kafkaExecutionListenerFactory(
            ConsumerFactory<String, ExecutionEvent> executionConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, ExecutionEvent> f = new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(executionConsumerFactory);
        f.setCommonErrorHandler(commonErrorHandler());
        f.setBatchListener(false);
        return f;
    }

    @Bean(name = "kafkaOrderStatusListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OrderStatusEvent> kafkaOrderStatusListenerFactory(
            ConsumerFactory<String, OrderStatusEvent> orderStatusConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, OrderStatusEvent> f = new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(orderStatusConsumerFactory);
        f.setCommonErrorHandler(commonErrorHandler());
        f.setBatchListener(false);
        return f;
    }

    @Bean(name = "kafkaStringListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaStringListenerFactory(
            ConsumerFactory<String, String> stringConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> f = new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(stringConsumerFactory);
        f.setCommonErrorHandler(commonErrorHandler());
        f.setBatchListener(false);
        return f;
    }
}
