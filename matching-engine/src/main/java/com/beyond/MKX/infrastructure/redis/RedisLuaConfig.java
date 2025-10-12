package com.beyond.MKX.infrastructure.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

/**
 * Redis Lua 스크립트 빈 등록 설정.
 * - 클래스패스의 lua/market_match.lua 를 로드하여 스프링에서 재사용 가능하도록 Bean화
 * - 반환 타입을 List로 지정(스크립트가 [remaining, count, ...] 형식 리스트를 반환)
 */
@Configuration
public class RedisLuaConfig {

    /**
     * 매칭용 Lua 스크립트 등록.
     * - 위치: classpath:lua/market_match.lua
     * - 결과 타입: List (스프링이 실행 결과를 List로 역직렬화)
     * - 스크립트 경로/파일명 변경 시 여기의 ClassPathResource도 함께 갱신 필요
     */
    @Bean
    public DefaultRedisScript<List> marketMatchScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/market_match.lua"));
        script.setResultType(List.class);
        return script;
    }
}
