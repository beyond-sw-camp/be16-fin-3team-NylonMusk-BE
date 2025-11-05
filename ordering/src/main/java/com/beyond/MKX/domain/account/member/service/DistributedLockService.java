package com.beyond.MKX.domain.account.member.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 분산 락 서비스 (Redis 기반)
 * 
 * 같은 accountId의 주문을 순차 처리하기 위한 락
 * - accountId 기반으로 락 획득
 * - TTL로 데드락 방지
 * - try-with-resources 패턴으로 자동 해제
 */
@Slf4j
@Service
public class DistributedLockService {

    private final StringRedisTemplate redisTemplate;

    public DistributedLockService(@Qualifier("idempotency") StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    private static final String LOCK_PREFIX = "lock:account:";
    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofSeconds(10); // 주문 처리 예상 시간

    /**
     * 계좌 기반 락 획득 시도
     * @param accountId 계좌 ID
     * @param timeout 락 타임아웃 (기본 10초)
     * @return 락 획득 성공 시 true, 실패 시 false
     */
    public boolean tryLock(UUID accountId, Duration timeout) {
        String lockKey = LOCK_PREFIX + accountId.toString();
        Duration lockDuration = timeout != null ? timeout : DEFAULT_LOCK_TIMEOUT;
        
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
            lockKey, 
            "LOCKED", 
            lockDuration.toSeconds(), 
            TimeUnit.SECONDS
        );
        
        if (Boolean.TRUE.equals(acquired)) {
            log.debug("Lock acquired for accountId: {}", accountId);
            return true;
        } else {
            log.debug("Lock acquisition failed for accountId: {} (already locked)", accountId);
            return false;
        }
    }

    /**
     * 계좌 기반 락 획득 시도 (기본 타임아웃 사용)
     */
    public boolean tryLock(UUID accountId) {
        return tryLock(accountId, DEFAULT_LOCK_TIMEOUT);
    }

    /**
     * 락 해제
     */
    public void unlock(UUID accountId) {
        String lockKey = LOCK_PREFIX + accountId.toString();
        redisTemplate.delete(lockKey);
        log.debug("Lock released for accountId: {}", accountId);
    }

    /**
     * 락 획득 및 자동 해제를 위한 락 리소스
     * try-with-resources 패턴으로 사용
     */
    public static class LockResource implements AutoCloseable {
        private final DistributedLockService lockService;
        private final UUID accountId;
        private final boolean acquired;

        public LockResource(DistributedLockService lockService, UUID accountId, boolean acquired) {
            this.lockService = lockService;
            this.accountId = accountId;
            this.acquired = acquired;
        }

        public boolean isAcquired() {
            return acquired;
        }

        @Override
        public void close() {
            if (acquired) {
                lockService.unlock(accountId);
            }
        }
    }

    /**
     * 락 획득 시도 후 LockResource 반환
     * try-with-resources로 사용하면 자동 해제
     */
    public LockResource acquireLock(UUID accountId, Duration timeout) {
        boolean acquired = tryLock(accountId, timeout);
        return new LockResource(this, accountId, acquired);
    }

    public LockResource acquireLock(UUID accountId) {
        return acquireLock(accountId, DEFAULT_LOCK_TIMEOUT);
    }
}

