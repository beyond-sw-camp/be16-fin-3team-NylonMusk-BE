package com.beyond.MKX.domain.account.member.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 멱등 처리 서비스(간단 Redis 기반)
 *
 * 키 공간
 * - idem:{key} = PENDING(예약) 또는 RESULT:{payload}
 *
 * 사용 패턴
 * 1) reserveOrGet(key, ttl) → 최초 호출이면 예약(PENDING), 이미 값이 있으면 해당 값 반환
 * 2) 성공 시 storeResult(key, payload, ttl)
 * 3) 실패 시 release(key)
 */
@Service
public class IdempotencyService {

    private final StringRedisTemplate redis;

    public IdempotencyService(@Qualifier("idempotency") StringRedisTemplate redis) {

        this.redis = redis;
    }

    private String key(String raw) { return "idem:" + raw; }

    /**
     * 예약 시도
     * - 최초 호출: PENDING으로 선점하고 Optional.empty 반환
     * - 중복 호출: 기존 값(PENDING/RESULT:...)을 Optional로 반환
     */
    public Optional<String> reserveOrGet(String idempotencyKey, Duration pendingTtl) {
        String namespaced = key(idempotencyKey);
        Boolean ok = redis.opsForValue().setIfAbsent(namespaced, "PENDING", pendingTtl);
        if (Boolean.TRUE.equals(ok)) {
            return Optional.empty(); // 선점 성공
        }
        String val = redis.opsForValue().get(namespaced);
        return Optional.ofNullable(val);
    }

    /**
     * 결과 저장
     * - RESULT:{payload} 형태로 저장
     * - TTL 부여로 캐시 기간 관리
     */
    public void storeResult(String idempotencyKey, String resultPayload, Duration resultTtl) {
        redis.opsForValue().set(key(idempotencyKey), "RESULT:" + resultPayload, resultTtl);
    }

    /**
     * 예약 해제(실패 시 롤백)
     */
    public void release(String idempotencyKey) {
        redis.delete(key(idempotencyKey));
    }
}
