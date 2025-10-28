package com.beyond.MKX.domain.orderbook.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 호가 업데이트 이벤트
 * 
 * OrderBookService에서 호가가 업데이트될 때 발행되는 이벤트
 * WebSocketHandler가 이 이벤트를 수신하여 클라이언트에게 전송
 */
@Getter
public class OrderBookUpdateEvent extends ApplicationEvent {
    
    private final String ticker;
    private final UpdateType updateType;
    
    public OrderBookUpdateEvent(Object source, String ticker, UpdateType updateType) {
        super(source);
        this.ticker = ticker;
        this.updateType = updateType;
    }
    
    /**
     * 업데이트 타입
     */
    public enum UpdateType {
        BASIC,      // 기본 호가 업데이트
        ENHANCED    // 고도화된 호가 업데이트
    }
}
