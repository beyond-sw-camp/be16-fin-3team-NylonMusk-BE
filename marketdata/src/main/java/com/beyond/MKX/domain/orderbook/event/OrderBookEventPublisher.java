package com.beyond.MKX.domain.orderbook.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 호가 이벤트 발행자
 * 
 * OrderBookService가 호가를 업데이트한 후 이벤트를 발행
 * 이를 통해 WebSocketHandler와의 순환 의존성을 제거
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderBookEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 기본 호가 업데이트 이벤트 발행
     * 
     * @param ticker 종목코드
     */
    public void publishBasicUpdate(String ticker) {
        OrderBookUpdateEvent event = new OrderBookUpdateEvent(
            this, 
            ticker, 
            OrderBookUpdateEvent.UpdateType.BASIC
        );
        eventPublisher.publishEvent(event);
        log.debug("[EVENT-PUBLISHER] Published BASIC update event: ticker={}", ticker);
    }
    
    /**
     * 고도화된 호가 업데이트 이벤트 발행
     * 
     * @param ticker 종목코드
     */
    public void publishEnhancedUpdate(String ticker) {
        try {
            OrderBookUpdateEvent event = new OrderBookUpdateEvent(
                this, 
                ticker, 
                OrderBookUpdateEvent.UpdateType.ENHANCED
            );
            eventPublisher.publishEvent(event);
            log.info("[EVENT-PUBLISHER] 📢 Published ENHANCED update event: ticker={}", ticker);
        } catch (Exception e) {
            log.error("[EVENT-PUBLISHER] ❌ Failed to publish ENHANCED update event: ticker={}", ticker, e);
        }
    }
}
