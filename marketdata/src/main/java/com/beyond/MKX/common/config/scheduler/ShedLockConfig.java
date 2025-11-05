package com.beyond.MKX.common.config.scheduler;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ShedLock 설정
 * 
 * MSA 환경에서 여러 인스턴스가 동시에 스케줄러를 실행하는 것을 방지
 * Redis를 사용한 분산 락(Distributed Lock) 구현
 * 
 * 주요 기능:
 * - lockAtMostFor: 락의 최대 유지 시간 (작업이 실패하더라도 이 시간 후 자동 해제)
 * - lockAtLeastFor: 락의 최소 유지 시간 (작업이 빨리 끝나도 이 시간 동안은 락 유지)
 * 
 * 예시:
 * @SchedulerLock(name = "schedulerName", lockAtMostFor = "10m", lockAtLeastFor = "5m")
 * 
 * 주의사항:
 * - 각 스케줄러의 name은 유니크해야 함
 * - lockAtMostFor는 작업의 예상 최대 실행 시간보다 길어야 함
 * - lockAtLeastFor는 작업의 최소 간격을 보장함
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

    /**
     * Redis 기반 LockProvider 생성
     * 
     * @param connectionFactory Redis 연결 팩토리
     * @return LockProvider 인스턴스
     */
    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "marketdata");
    }
}
