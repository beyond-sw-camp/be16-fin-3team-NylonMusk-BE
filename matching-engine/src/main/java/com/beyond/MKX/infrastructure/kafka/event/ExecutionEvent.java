package com.beyond.MKX.infrastructure.kafka.event;

import lombok.*;

@Builder
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExecutionEvent {
    private String execId;
    private String marketOrderId;
    private String counterOrderId;
    private String ticker;
    private String side;     // BUY/SELL (시장주문 방향)
    private double price;
    private double quantity;
    private long timestamp;
}
