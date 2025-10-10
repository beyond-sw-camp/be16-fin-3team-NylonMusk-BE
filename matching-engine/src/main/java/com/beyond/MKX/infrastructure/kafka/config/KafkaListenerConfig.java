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

@EnableKafka
@Configuration
public class KafkaListenerConfig {

    private Map<String, Object> baseConsumerConfigs(KafkaProperties properties) {
        Map<String, Object> configs = new HashMap<>(properties.buildConsumerProperties(null));
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        // 실제 delegate 지정
        configs.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());
        configs.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        // JSON 공통 설정
        configs.put(JsonDeserializer.TRUSTED_PACKAGES, "*");           // 패키지 신뢰
        configs.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);     // 타입 헤더 사용 (프로듀서가 붙임)
        return configs;
    }

    // ---- executions 전용 ConsumerFactory (ExecutionEvent) ----
    @Bean
    public ConsumerFactory<String, ExecutionEvent> executionConsumerFactory(KafkaProperties properties) {
        Map<String, Object> configs = baseConsumerConfigs(properties);
        // 기본 타입 고정 (헤더가 없거나 문자열일 때도 안전)
        configs.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ExecutionEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    // ---- order-status 전용 ConsumerFactory (OrderStatusEvent) ----
    @Bean
    public ConsumerFactory<String, OrderStatusEvent> orderStatusConsumerFactory(KafkaProperties properties) {
        Map<String, Object> configs = baseConsumerConfigs(properties);
        configs.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderStatusEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    // ---- 문자열 전용 ConsumerFactory (에러 토픽 등) ----
    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory(KafkaProperties properties) {
        Map<String, Object> configs = new HashMap<>(properties.buildConsumerProperties(null));
        // 문자열은 굳이 ErrorHandlingDeserializer 필요 없음 (원하면 동일 패턴 적용 가능)
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    // 공통 에러 핸들러: 역직렬화/처리 중 에러 시 백오프 후 스킵
    private DefaultErrorHandler commonErrorHandler() {
        // 초기 100ms, 배수 2.0, 최대 5회 재시도 후 레코드 스킵
        ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(5);
        backoff.setInitialInterval(100);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(2_000);
        return new DefaultErrorHandler(backoff);
    }

    // ---- 컨테이너 공장들 ----
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
