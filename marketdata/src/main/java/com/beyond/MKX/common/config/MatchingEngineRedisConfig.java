package com.beyond.MKX.common.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

/**
 * Matching Engine Redis Cluster 연결 설정
 * 
 * matching-engine 서비스와 동일한 Redis Cluster에 연결하여
 * orderbook 데이터를 직접 조회
 */
@Slf4j
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "matching-engine.redis.cluster")
@Getter
@Setter
public class MatchingEngineRedisConfig {

    private List<String> nodes;
    private int maxRedirects = 5;

    @Bean(name = "matchingEngineRedisConnectionFactory")
    public RedisConnectionFactory matchingEngineRedisConnectionFactory() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(nodes);
        clusterConfig.setMaxRedirects(maxRedirects);

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

        LettuceConnectionFactory factory = new LettuceConnectionFactory(clusterConfig, clientConfig);
        factory.afterPropertiesSet();
        
        log.info("[MATCHING-ENGINE-REDIS] ✅ Connected to Redis Cluster: nodes={}", nodes);
        
        return factory;
    }

    @Bean(name = "matchingEngineRedisTemplate")
    public StringRedisTemplate matchingEngineRedisTemplate(
            @Qualifier("matchingEngineRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }
}

