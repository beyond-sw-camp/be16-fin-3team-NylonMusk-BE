package com.beyond.MKX.domain.execution.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * 체결 이벤트 DTO
 * 
 * matching-engine의 ExecutionEvent와 동일한 구조
 * Kafka로부터 수신하는 체결 데이터 모델
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExecutionEventDTO {
    private String execId;           // 체결 식별자
    private String marketOrderId;    // 시장(진입) 주문 ID
    private String counterOrderId;   // 상대(대응) 주문 ID
    private String ticker;           // 종목 코드
    private String side;             // BUY/SELL
    private long price;              // 체결 가격
    private BigDecimal quantity;     // 체결 수량
    private long timestamp;          // 체결 시각(epoch millis)
}
