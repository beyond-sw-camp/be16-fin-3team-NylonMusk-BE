// src/main/java/com/beyond/MKX/global/config/ScheduleAsyncConfig.java
package com.beyond.MKX.common.config;

import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
@EnableAsync
public class ScheduleAsyncConfig {

    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean(name = "marketFetcherExecutor")
    public Executor marketFetcherExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("market-fetch-");
        ex.initialize();
        return ex;
    }
}
