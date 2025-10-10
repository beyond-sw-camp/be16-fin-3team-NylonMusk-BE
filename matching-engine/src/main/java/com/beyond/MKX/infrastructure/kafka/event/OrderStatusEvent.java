package com.beyond.MKX.infrastructure.kafka.event;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderStatusEvent {
    private String orderId;
    private String status;     // NEW_ACCEPTED / MARKET_PARTIAL / MARKET_FILLED / WAITING / CANCEL_OK ...
    private String ticker;     // optional
    private String side;       // optional
    private double price;      // optional
    private double remaining;  // optional
    private long timestamp;
    private double avgFillPrice;
    private double lastFillPrice;
    private double limitPrice;
    private double filledQuantity;
}
