package com.beyond.MKX.common.config;

import com.beyond.MKX.domain.websocket.pubsub.RedisPubSubListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 설정 (Redis Pub/Sub 기반으로 전환)
 *
 * STOMP와 함께 사용하기 위한 Redis Pub/Sub 설정
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setDatabase(database);
        
        // ✅ Lettuce 클라이언트 설정 개선
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofSeconds(10))
                .keepAlive(true)
                .build();
        
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .autoReconnect(true)
                .build();
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofSeconds(5))
                .build();
        
        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> jsonSerializer = 
                new Jackson2JsonRedisSerializer<>(redisObjectMapper, Object.class);

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis Pub/Sub 메시지 리스너 컨테이너
     *
     * Public 채널 패턴:
     * - market:orderbook - 호가 데이터
     * - market:trades - 체결 데이터
     * - market:price - 현재가 데이터
     * - market:chart - 차트 데이터
     * - market:indicator - 보조지표 데이터
     * - market:summary - 시장 요약
     *
     * Private 채널 패턴:
     * - user:* - 사용자별 개인 데이터
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisPubSubListener redisPubSubListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // Public 채널 구독 - 간소화된 패턴
        container.addMessageListener(redisPubSubListener, new PatternTopic("market:orderbook"));
        container.addMessageListener(redisPubSubListener, new PatternTopic("market:trades"));
        container.addMessageListener(redisPubSubListener, new PatternTopic("market:price"));
        container.addMessageListener(redisPubSubListener, new PatternTopic("market:chart"));
        container.addMessageListener(redisPubSubListener, new PatternTopic("market:indicator"));
        container.addMessageListener(redisPubSubListener, new PatternTopic("market:summary"));

        // Private 채널 구독 (사용자별)
        container.addMessageListener(redisPubSubListener, new PatternTopic("user:*"));

        return container;
    }
}
