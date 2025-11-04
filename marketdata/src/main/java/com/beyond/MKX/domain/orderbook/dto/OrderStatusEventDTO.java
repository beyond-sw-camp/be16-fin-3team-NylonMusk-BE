package com.beyond.MKX.domain.orderbook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;

/**
 * 주문 상태 이벤트 DTO
 *
 * matching-engine의 OrderStatusEvent와 동일한 구조
 * Kafka order-status 토픽에서 수신하는 주문 상태 변화 이벤트
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderStatusEventDTO {
    private String orderId;           // 주문 식별자
    private String status;            // NEW_ACCEPTED, MARKET_PARTIAL, MARKET_FILLED, WAITING, CANCEL_OK 등
    private String ticker;            // 종목 코드
    private String side;              // BUY/SELL
    private long price;               // 주문 가격
    private BigDecimal remaining;     // 남은 수량
    private long timestamp;           // 이벤트 생성 시각
    private long avgFillPrice;        // 체결 평균가
    private long lastFillPrice;       // 마지막 체결가
    private long limitPrice;          // 지정가
    private long filledQuantity;// 누적 체결 수량
    private long notional;            // 총 거래대금
}
