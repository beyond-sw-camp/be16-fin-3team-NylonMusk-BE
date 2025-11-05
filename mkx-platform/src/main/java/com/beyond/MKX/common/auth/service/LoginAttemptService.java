package com.beyond.MKX.common.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * LOGIN_ATTEMPT_SERVICE: 로그인 시도 횟수 관리 서비스
 * - Redis에 로그인 실패 횟수 저장 (database 5)
 * - 5회 이상 실패 시 CAPTCHA 필수
 * - 로그인 성공 시 실패 횟수 초기화
 */
@Slf4j
@Service
public class LoginAttemptService {

    private final RedisTemplate<String, Object> redisTemplate;

    public LoginAttemptService(@Qualifier("emailQueue") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** LOGIN_ATTEMPT: Redis 키 prefix */
    private static final String KEY_PREFIX = "login:failed:";

    /** MAX_ATTEMPTS: 최대 실패 횟수 (5회) */
    private static final int MAX_ATTEMPTS = 5;

    /** TTL: 실패 횟수 저장 TTL (24시간) */
    private static final long TTL_HOURS = 24;

    /**
     * INCREMENT_FAILED_ATTEMPTS: 로그인 실패 횟수 증가
     * @param email 이메일 주소
     * @return 현재 실패 횟수
     */
    public int incrementFailedAttempts(String email) {
        String key = KEY_PREFIX + email;
        
        // Redis에서 현재 횟수 조회
        Object countObj = redisTemplate.opsForValue().get(key);
        int currentCount = countObj != null ? Integer.parseInt(countObj.toString()) : 0;
        
        // 횟수 증가
        int newCount = currentCount + 1;
        redisTemplate.opsForValue().set(key, String.valueOf(newCount), Duration.ofHours(TTL_HOURS));
        
        log.info("로그인 실패 횟수 증가: email={}, count={}", email, newCount);
        return newCount;
    }

    /**
     * GET_FAILED_ATTEMPTS: 현재 실패 횟수 조회
     * @param email 이메일 주소
     * @return 실패 횟수
     */
    public int getFailedAttempts(String email) {
        String key = KEY_PREFIX + email;
        Object countObj = redisTemplate.opsForValue().get(key);
        return countObj != null ? Integer.parseInt(countObj.toString()) : 0;
    }

    /**
     * REQUIRES_CAPTCHA: CAPTCHA 필요 여부 확인 (5회 이상 실패)
     * @param email 이메일 주소
     * @return CAPTCHA 필요 여부
     */
    public boolean requiresCaptcha(String email) {
        int failedAttempts = getFailedAttempts(email);
        return failedAttempts >= MAX_ATTEMPTS;
    }

    /**
     * RESET_FAILED_ATTEMPTS: 로그인 성공 시 실패 횟수 초기화
     * @param email 이메일 주소
     */
    public void resetFailedAttempts(String email) {
        String key = KEY_PREFIX + email;
        redisTemplate.delete(key);
        log.info("로그인 실패 횟수 초기화: email={}", email);
    }

    /**
     * GET_REMAINING_ATTEMPTS: 남은 시도 횟수 조회
     * @param email 이메일 주소
     * @return 남은 시도 횟수
     */
    public int getRemainingAttempts(String email) {
        int failedAttempts = getFailedAttempts(email);
        return Math.max(0, MAX_ATTEMPTS - failedAttempts);
    }
}
