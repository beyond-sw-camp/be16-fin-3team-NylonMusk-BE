package com.beyond.MKX.common.kafka.event;

import lombok.*;

import java.math.BigDecimal;

/**
 * 체결(Execution) 이벤트 DTO.
 *
 * 사용처
 * - 매칭 엔진이 체결 발생 시 Kafka "executions" 토픽으로 전송하는 페이로드 모델
 * - ordering-service 체결 이벤트 컨슘
 *
 * 필드 설명
 * - execId         : 체결 식별자(멱등 키 구성에 사용 가능)
 * - marketOrderId  : 시장(진입) 주문 ID
 * - counterOrderId : 상대(대응) 주문 ID
 * - ticker         : 종목 코드
 * - side           : BUY/SELL (시장 주문의 방향)
 * - price          : 체결 가격
 * - quantity       : 체결 수량
 * - timestamp      : 이벤트 생성 시각(epoch millis)
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExecutionEvent {
    private String execId;
    private String marketOrderId;
    private String counterOrderId;
    private String ticker;
    private String side;     // BUY/SELL (시장주문 방향)
    private long price;
    private BigDecimal quantity;
    private long timestamp;
}
