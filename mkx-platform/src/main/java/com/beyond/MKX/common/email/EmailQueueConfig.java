package com.beyond.MKX.common.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * EMAIL_QUEUE_CONFIG: 이메일 발송을 위한 비동기 스레드 풀 설정
 * - 병렬 이메일 발송을 위한 ThreadPoolTaskExecutor 구성
 */
@Slf4j
@Configuration
@EnableAsync
public class EmailQueueConfig {

    /**
     * EMAIL_TASK_EXECUTOR: 이메일 발송 전용 ThreadPool
     * 
     * - Core Pool Size: 3 (기본 스레드 수)
     * - Max Pool Size: 5 (최대 스레드 수, Gmail SMTP 동시 연결 제한 고려)
     * - Queue Capacity: 50 (대기 큐 크기)
     * - Keep Alive: 60초
     * 
     * NOTE: Gmail SMTP 동시 연결 제한 (공식 문서 없으나 일반적으로 10-20개)
     *       안전을 위해 최대 5개 스레드로 제한하여 계정 정지 위험 최소화
     */
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 기본 스레드 수 (Gmail SMTP 안정성을 위해 보수적으로 설정)
        executor.setCorePoolSize(3);
        
        // 최대 스레드 수 (Gmail 동시 연결 제한 고려: 보통 10-20개, 안전하게 5개로 제한)
        executor.setMaxPoolSize(5);
        
        // 큐 용량
        executor.setQueueCapacity(50);
        
        // 스레드 이름 접두사
        executor.setThreadNamePrefix("Email-Async-");
        
        // Keep Alive Time (초)
        executor.setKeepAliveSeconds(60);
        
        // 거부 정책: CallerRunsPolicy (호출한 스레드에서 실행)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 종료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("[EMAIL-CONFIG] ✅ Email Task Executor initialized: " +
                "corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                executor.getCorePoolSize(), 
                executor.getMaxPoolSize(), 
                executor.getQueueCapacity());
        
        return executor;
    }
}

