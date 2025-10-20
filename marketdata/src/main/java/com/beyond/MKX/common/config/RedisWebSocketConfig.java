package com.beyond.MKX.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis WebSocket 전용 설정
 * 
 * WebSocket 메시지 전송을 위한 StringRedisTemplate
 * JSON 직렬화 없이 순수 문자열로 저장하여 인용부호 문제 해결
 */
@Configuration
public class RedisWebSocketConfig {

    /**
     * WebSocket용 StringRedisTemplate
     * 
     * 모든 값을 String으로 저장하여 JSON 직렬화로 인한 인용부호 추가 방지
     */
    @Bean(name = "webSocketRedisTemplate")
    public StringRedisTemplate webSocketRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }
}
