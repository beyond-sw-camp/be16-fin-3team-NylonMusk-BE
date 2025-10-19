package com.beyond.MKX.common.config;

import com.beyond.MKX.domain.chart.listener.WebSocketMessageListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 기반 WebSocket 메시지 브로커 설정
 * 
 * MSA 환경에서 여러 인스턴스 간 WebSocket 메시지를 공유하기 위한 Redis Pub/Sub 설정
 * 각 인스턴스는 로컬 WebSocket 세션을 관리하고, Redis를 통해 메시지를 브로드캐스트
 */
@Configuration
@RequiredArgsConstructor
public class RedisWebSocketConfig {

    private final ObjectMapper objectMapper;

    /**
     * Redis 메시지 리스너 컨테이너
     * websocket:* 패턴의 채널을 구독하여 메시지 수신
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // websocket:chart:*, websocket:orderbook:*, websocket:price:* 채널 구독
        container.addMessageListener(listenerAdapter, new PatternTopic("websocket:*"));
        
        return container;
    }

    /**
     * 메시지 리스너 어댑터
     * Redis로부터 받은 메시지를 WebSocketMessageListener의 onMessage(String, String) 메서드로 전달
     */
    @Bean
    public MessageListenerAdapter listenerAdapter(WebSocketMessageListener listener) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(listener, "onMessage");
        // String으로 역직렬화
        adapter.setSerializer(new StringRedisSerializer());
        return adapter;
    }

    /**
     * WebSocket 메시지 발행용 RedisTemplate
     * JSON 직렬화를 사용하여 객체를 Redis에 저장/전송
     */
    @Bean(name = "webSocketRedisTemplate")
    public RedisTemplate<String, Object> webSocketRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key는 String으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value는 JSON으로 직렬화
        GenericJackson2JsonRedisSerializer serializer = 
                new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * WebSocket 세션 관리용 RedisTemplate
     * 세션 정보를 Redis에 저장하여 인스턴스 간 공유
     */
    @Bean(name = "sessionRedisTemplate")
    public RedisTemplate<String, String> sessionRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 모두 String으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
