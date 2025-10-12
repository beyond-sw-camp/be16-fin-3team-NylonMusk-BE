package com.beyond.MKX.common.redis;

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
public class FeePolicyRedisConfig {

    private final CommonRedisProps props;

    public FeePolicyRedisConfig(CommonRedisProps props) {
        this.props = props;
    }

    /// **------------ 증권사 수수료 및 거래세 redis 테이블 config 설정 ------------**
    // 수수료 및 거래세 DB
    @Bean(name = "fee-policy")
    @Primary
    public RedisConnectionFactory feePolicyFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(props.getHost());
        configuration.setPort(props.getPort());
        configuration.setDatabase(1);
        return new LettuceConnectionFactory(configuration);
    }

    @Bean(name = "feePolicyTemplate")
    public RedisTemplate<String, String> feePolicyTemplate(
            @Qualifier("fee-policy") RedisConnectionFactory redisConnectionFactory
    ) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }


}
