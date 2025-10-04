package com.beyond.MKX.common.auth.repository;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RevokedTokenRepository {

    @Qualifier("rtdb")
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_RT = "rt:";               // rt:{userId} → 현재 RefreshToken
    private static final String KEY_BLACKLIST = "rt:blacklist:"; // rt:blacklist:{jti} → 만료 전 폐기된 RT


    //사용자별 RT 저장
    public void saveRefreshToken(UUID userId, String rt, long ttlMillis) {
        redisTemplate.opsForValue()
                .set(KEY_RT + userId, rt, Duration.ofMillis(ttlMillis));
    }


    // 사용자별 RT 조회
    public String getRefreshToken(UUID userId) {
        Object rt = redisTemplate.opsForValue().get(KEY_RT + userId);
        return rt != null ? rt.toString() : null;
    }


    // RefreshToken 블랙리스트 등록
    public void revoke(String jti, long ttlMillis) {
        if (jti == null) return;
        redisTemplate.opsForValue()
                .set(KEY_BLACKLIST + jti, "1", Duration.ofMillis(ttlMillis));
    }


    // jti 블랙리스트 여부 확인
    public boolean isRevoked(String jti) {
        return jti == null || Boolean.TRUE.equals(redisTemplate.hasKey(KEY_BLACKLIST + jti));
    }

    // 사용자별 RT 삭제 (로그아웃, 강제만료 등)
    public void deleteRefreshToken(UUID userId) {
        redisTemplate.delete(KEY_RT + userId);
    }
}