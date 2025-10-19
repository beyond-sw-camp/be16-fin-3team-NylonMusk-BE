package com.beyond.MKX.common.config;

import com.beyond.MKX.domain.execution.dto.ExecutionEventDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer 설정
 * 
 * matching-engine에서 발송하는 체결 이벤트와 주문 상태 이벤트를 수신하기 위한 설정
 * - executions 토픽: 체결 이벤트 (ExecutionEventDTO)
 * - order-status 토픽: 주문 상태 이벤트 (OrderStatusEventDTO)
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * 체결 이벤트용 Consumer Factory
     */
    @Bean
    public ConsumerFactory<String, ExecutionEventDTO> executionConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // JsonDeserializer 설정
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ExecutionEventDTO.class.getName());
        
        // Consumer 설정
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(ExecutionEventDTO.class, false)
        );
    }

    /**
     * 체결 이벤트용 Listener Container Factory (기본)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ExecutionEventDTO> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ExecutionEventDTO> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(executionConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    /**
     * 주문 상태 이벤트용 Consumer Factory
     */
    @Bean
    public ConsumerFactory<String, com.beyond.MKX.domain.orderbook.dto.OrderStatusEventDTO> orderStatusConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // JsonDeserializer 설정
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, com.beyond.MKX.domain.orderbook.dto.OrderStatusEventDTO.class.getName());
        
        // Consumer 설정
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(com.beyond.MKX.domain.orderbook.dto.OrderStatusEventDTO.class, false)
        );
    }

    /**
     * 주문 상태 이벤트용 Listener Container Factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.beyond.MKX.domain.orderbook.dto.OrderStatusEventDTO> orderStatusKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, com.beyond.MKX.domain.orderbook.dto.OrderStatusEventDTO> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderStatusConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
