package com.beyond.MKX.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {
    private static final int PARTITIONS = 4;
    private static final short REPLICA  = 1;

    @Bean public NewTopic executionsTopic()   { return new NewTopic("executions",   PARTITIONS, REPLICA); }
    @Bean public NewTopic orderStatusTopic()  { return new NewTopic("order-status", PARTITIONS, REPLICA); }
    @Bean public NewTopic orderErrorsTopic()  { return new NewTopic("order-errors", PARTITIONS, REPLICA); }
}
