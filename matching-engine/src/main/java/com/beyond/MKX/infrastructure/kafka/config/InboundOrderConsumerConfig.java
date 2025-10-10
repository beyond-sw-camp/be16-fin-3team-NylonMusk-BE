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

@EnableKafka
@Configuration
public class InboundOrderConsumerConfig {

    @Bean
    public ConsumerFactory<String, InboundOrderMessage> inboundOrderConsumerFactory(KafkaProperties props) {
        Map<String, Object> cfg = new HashMap<>(props.buildConsumerProperties(null));

        // ErrorHandlingDeserializer로 역직렬화 오류를 안전하게 처리
        cfg.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        cfg.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        cfg.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        cfg.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // Json 역직렬화 대상 타입 지정 + 신뢰 패키지
        cfg.put(JsonDeserializer.VALUE_DEFAULT_TYPE, InboundOrderMessage.class.getName());
        cfg.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(cfg);
    }

    @Bean(name = "inboundOrderListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, InboundOrderMessage> inboundOrderListenerFactory(
            ConsumerFactory<String, InboundOrderMessage> cf) {
        ConcurrentKafkaListenerContainerFactory<String, InboundOrderMessage> f =
                new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(cf);
        return f;
    }
}
