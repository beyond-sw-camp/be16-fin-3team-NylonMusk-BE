package com.beyond.MKX.common.config.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;

/**
 * STOMP WebSocket 설정
 *
 * Redis Pub/Sub 기반의 STOMP 메시징 설정
 * Public/Private 채널 분리 구조
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final OptionalAuthChannelInterceptor optionalAuthChannelInterceptor;
    private final RateLimitingInterceptor rateLimitingInterceptor;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * STOMP 엔드포인트 등록
     *
     * SockJS fallback 지원
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:3001",
                        "https://yourdomain.com"
                )
                .withSockJS();

        log.info("[STOMP] ✅ Registered STOMP endpoint: /ws with SockJS");
    }

    /**
     * 메시지 브로커 설정
     *
     * Simple Broker 사용 (Redis Pub/Sub는 별도로 처리)
     * - /topic/* : Public 채널 (인증 불필요)
     * - /queue/* : Private 채널 (인증 필수)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple In-Memory Broker 활성화
        // Redis Pub/Sub는 RedisPubSubListener에서 별도로 처리
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})  // 10초 heartbeat
                .setTaskScheduler(heartbeatScheduler());  // ✅ TaskScheduler 추가
        
        // 클라이언트 → 서버 메시지 prefix
        registry.setApplicationDestinationPrefixes("/app");

        // 특정 사용자에게 메시지 전송 시 prefix
        registry.setUserDestinationPrefix("/user");

        log.info("[STOMP] ✅ Message broker configured");
        log.info("[STOMP]   - Public channels: /topic/*");
        log.info("[STOMP]   - Private channels: /queue/*");
        log.info("[STOMP]   - App prefix: /app");
        log.info("[STOMP]   - User prefix: /user");
    }

    /**
     * WebSocket Heartbeat용 TaskScheduler Bean 등록
     */
    @Bean
    public TaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * 클라이언트 인바운드 채널 설정
     *
     * Optional 인증 인터셉터 + Rate Limiting 적용
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                optionalAuthChannelInterceptor,
                rateLimitingInterceptor
        );

        log.info("[STOMP] ✅ Client inbound channel configured with interceptors");
    }

    /**
     * WebSocket 전송 설정
     *
     * 타임아웃 및 버퍼 크기 설정
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(8192)           // 8KB
                .setSendBufferSizeLimit(512 * 1024)  // 512KB
                .setSendTimeLimit(20 * 1000);        // 20초

        log.info("[STOMP] ✅ WebSocket transport configured");
    }
}