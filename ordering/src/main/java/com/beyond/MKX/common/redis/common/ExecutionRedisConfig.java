package com.beyond.MKX.common.redis.common;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableConfigurationProperties(CommonRedisProps.class)
public class ExecutionRedisConfig {

    private final CommonRedisProps props;

    public ExecutionRedisConfig(CommonRedisProps props) {
        this.props = props;
    }

    /// **------------ 시장가  환불 트래킹 용 redis 테이블 config 설정 ------------**
    //
    @Bean(name = "market-partial-refund")
    public RedisConnectionFactory marketPartialRefundFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(props.getHost());
        configuration.setPort(props.getPort());
        configuration.setDatabase(3);
        return new LettuceConnectionFactory(configuration);
    }

    @Bean(name = "marketPartialRefundTemplate")
    public RedisTemplate<String, String> marketPartialRefundTemplate(
            @Qualifier("market-partial-refund") RedisConnectionFactory redisConnectionFactory
    ) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }


}
