package com.beyond.MKX.common.kafka.config;

//import com.fasterxml.jackson.databind.JsonDeserializer;
import com.beyond.MKX.common.kafka.event.ExecutionEvent;
import com.beyond.MKX.common.kafka.event.OrderStatusEvent;
import com.beyond.MKX.common.kafka.event.TransactionEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumeConfig {

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
        configs.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);     // 프로듀서 타입 헤더 사용 시 'true' 값으로 변경 요함
        return configs;
    }


    /// **-------------- ConsumerFactory 설정 --------------**

    // ----------------------------------------------------------------------
    // executions 전용 ConsumerFactory (ExecutionEvent)
    //  - VALUE_DEFAULT_TYPE 고정: 헤더 부재/문자열 발행에도 안전
    // ----------------------------------------------------------------------
    @Bean
    public ConsumerFactory<String, ExecutionEvent> executionConsumerFactory(KafkaProperties properties) {
        Map<String, Object> configs = baseConsumerConfigs(properties);
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
    // transaction-events 전용 ConsumerFactory (TransactionEvent)
    // ----------------------------------------------------------------------
    @Bean
    public ConsumerFactory<String, TransactionEvent> transactionConsumerFactory(KafkaProperties properties) {
        Map<String, Object> configs = baseConsumerConfigs(properties);
        configs.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionEvent.class.getName());
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


    /// **-------------- KafkaListenerContainerFactory 설정 --------------**

    @Bean("kafkaExecutionListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, ExecutionEvent> kafkaExecutionListenerFactory(
            ConsumerFactory<String, ExecutionEvent> executionConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, ExecutionEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(executionConsumerFactory);
        factory.setCommonErrorHandler(commonErrorHandler());
        // yml의 enable-auto-commit: false 설정에 따른 수동 커밋 설정
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean("kafkaOrderStatusListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OrderStatusEvent> kafkaOrderStatusListenerFactory(
            ConsumerFactory<String, OrderStatusEvent> orderStatusConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderStatusEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderStatusConsumerFactory);
        factory.setCommonErrorHandler(commonErrorHandler());
        // yml의 enable-auto-commit: false 설정에 따른 수동 커밋 설정
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean("kafkaTransactionListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, TransactionEvent> kafkaTransactionListenerFactory(
            ConsumerFactory<String, TransactionEvent> transactionConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, TransactionEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transactionConsumerFactory);
        factory.setCommonErrorHandler(commonErrorHandler());
        // yml의 enable-auto-commit: false 설정에 따른 수동 커밋 설정
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    /// **-------------- 에러 핸들러 설정 --------------**

    private DefaultErrorHandler commonErrorHandler() {
        // 초기 100ms, 배수 2.0, 최대 5회 재시도 후 레코드 스킵
        ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(5);
        backoff.setInitialInterval(100);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(2_000);
        return new DefaultErrorHandler(backoff);
    }

}
