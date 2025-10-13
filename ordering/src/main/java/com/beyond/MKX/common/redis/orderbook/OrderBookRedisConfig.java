package com.beyond.MKX.common.redis.orderbook;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
@EnableConfigurationProperties(OrderBookRedisProps.class)
public class OrderBookRedisConfig {

    private final OrderBookRedisProps props;

    public OrderBookRedisConfig(OrderBookRedisProps props) {
        this.props = props;
    }

    @Bean("orderBookConnectionFactory")
    public LettuceConnectionFactory orderBookConnectionFactory() {
        var clusterCfg = new RedisClusterConfiguration(props.getCluster().getNodes());
        if (props.getCluster().getMaxRedirects() != null) {
            clusterCfg.setMaxRedirects(props.getCluster().getMaxRedirects());
        }
        var clientCfg = LettuceClientConfiguration.builder()
                .commandTimeout(props.getTimeout())
                .build();
        return new LettuceConnectionFactory(clusterCfg, clientCfg);
    }

    @Bean("orderBookRedisTemplate")
    public RedisTemplate<String, String> orderBookRedisTemplate(
            @Qualifier("orderBookConnectionFactory") RedisConnectionFactory redisConnectionFactory
    ) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();

        // 위에서 만든 ConnectionFactory를 설정해 줍니다.
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // Redis는 데이터를 바이트(byte) 배열로 저장하기 때문에,
        // 우리가 사용하는 String 객체를 바이트로, 또 바이트를 String으로 변환하는 방법을 명시해야 합니다.

        // Key Serializer
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // Value Serializer
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        // Hash Key Serializer (해시 타입을 사용할 경우)
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        // Hash Value Serializer (해시 타입을 사용할 경우)
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }


}
