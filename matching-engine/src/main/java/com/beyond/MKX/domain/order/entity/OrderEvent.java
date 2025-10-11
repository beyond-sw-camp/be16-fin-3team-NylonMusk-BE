package com.beyond.MKX.domain.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 브로커리지 → 매칭엔진으로 전달되는 "주문 이벤트" 전송 객체.
 * - Kafka/HTTP 등 경계를 넘어 이동하는 DTO 용도
 * - MatchingEngineService.process(...)의 입력 모델
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderEvent {
    private String brokerageId;   // 증권사 ID
    private String orderId;       // 주문 ID
    private String ticker;        // 종목 코드
    private String side;          // 매수(BUY)/매도(SELL)
    private String orderType;     // 주문 유형 (e.g. LIMIT, MARKET, CANCEL)
    private long price;         // 가격
    private BigDecimal quantity;      // 수량
    private LocalDateTime createdAt; // 생성 시각
}
