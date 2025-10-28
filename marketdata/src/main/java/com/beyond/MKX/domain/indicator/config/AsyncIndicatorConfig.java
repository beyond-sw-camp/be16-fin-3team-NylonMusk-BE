package com.beyond.MKX.domain.indicator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 지표 계산 설정
 * 
 * 여러 지표를 동시에 계산하기 위한 ThreadPool 설정
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncIndicatorConfig {

    /**
     * 지표 계산 전용 ThreadPool
     * 
     * - Core Pool Size: 10 (기본 스레드 수)
     * - Max Pool Size: 20 (최대 스레드 수)
     * - Queue Capacity: 100 (대기 큐 크기)
     * - Keep Alive: 60초
     */
    @Bean(name = "indicatorTaskExecutor")
    public Executor indicatorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 기본 스레드 수
        executor.setCorePoolSize(10);
        
        // 최대 스레드 수
        executor.setMaxPoolSize(20);
        
        // 큐 용량
        executor.setQueueCapacity(100);
        
        // 스레드 이름 접두사
        executor.setThreadNamePrefix("Indicator-Async-");
        
        // Keep Alive Time (초)
        executor.setKeepAliveSeconds(60);
        
        // 거부 정책: CallerRunsPolicy (호출한 스레드에서 실행)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 종료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("[ASYNC-CONFIG] ✅ Indicator Task Executor initialized: " +
                "corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                executor.getCorePoolSize(), 
                executor.getMaxPoolSize(), 
                executor.getQueueCapacity());
        
        return executor;
    }
}
