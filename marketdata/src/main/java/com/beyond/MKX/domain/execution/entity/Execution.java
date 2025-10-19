package com.beyond.MKX.domain.execution.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 체결 엔티티
 * 
 * InfluxDB에 저장될 체결 데이터 모델
 * InfluxDB는 시계열 데이터베이스로, measurement(테이블), tags(인덱스), fields(값)로 구성
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Execution {
    
    // Measurement name
    public static final String MEASUREMENT = "executions";
    
    // Tags (인덱싱되는 필드)
    private String ticker;           // 종목 코드
    private String side;             // BUY/SELL
    private String execId;           // 체결 식별자
    
    // Fields (값 필드)
    private String marketOrderId;    // 시장 주문 ID
    private String counterOrderId;   // 상대 주문 ID
    private long price;              // 체결 가격
    private BigDecimal quantity;     // 체결 수량
    
    // Timestamp
    private Instant timestamp;       // 체결 시각
}
