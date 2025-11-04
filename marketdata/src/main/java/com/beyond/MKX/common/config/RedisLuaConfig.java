package com.beyond.MKX.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

/**
 * Redis Lua Script 설정
 */
@Configuration
public class RedisLuaConfig {

    /**
     * Orderbook 조회 Lua 스크립트
     * matching-engine Redis에서 orderbook을 원자적으로 조회
     */
    @Bean
    @SuppressWarnings("rawtypes")
    public DefaultRedisScript<List> getOrderBookScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/get_orderbook.lua"));
        script.setResultType(List.class);
        return script;
    }
}

