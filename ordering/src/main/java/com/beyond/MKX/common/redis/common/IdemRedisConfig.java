package com.beyond.MKX.common.redis.common;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 설정(멱등 처리 용도)
 * - DB index: 2 (운영 환경과 충돌되지 않도록 분리)
 * - StringRedisTemplate만 제공(키/값 모두 문자열)
 */
@Configuration
@EnableConfigurationProperties(CommonRedisProps.class)
public class IdemRedisConfig {

    private final CommonRedisProps props;

    public IdemRedisConfig(CommonRedisProps props) {
        this.props = props;
    }

    /**
     * Lettuce 기반 Redis 커넥션 팩토리(DB 2)
     */
    @Bean(name = "idempotency")
    @Qualifier("idempotency")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(props.getHost());
        configuration.setPort(props.getPort());
        configuration.setDatabase(2);
        return new LettuceConnectionFactory(configuration);
    }

    /**
     * 멱등 처리용 StringRedisTemplate
     */
    @Bean(name = "redisTemplate")
    @Qualifier("idempotency")
    public StringRedisTemplate stringRedisTemplate(
            @Qualifier("idempotency") RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
