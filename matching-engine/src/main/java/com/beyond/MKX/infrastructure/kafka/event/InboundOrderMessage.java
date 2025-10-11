package com.beyond.MKX.infrastructure.kafka.event;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 외부(브로커리지/주문 발행 서비스) → 매칭 엔진으로 유입되는 인바운드 주문 메시지 DTO.
 *
 * 목적
 * - Kafka "place-order" 토픽의 메시지를 역직렬화하여 내부 도메인 모델(OrderEvent)로 매핑하기 전 단계에서 사용
 * - 스키마가 추가/변경되더라도 역직렬화 오류를 피하기 위해 알 수 없는 필드를 무시(@JsonIgnoreProperties)
 *
 * 필드 설명
 * - brokerageId : 증권사/발행 시스템 식별자
 * - orderId     : 주문 고유 ID
 * - ticker      : 종목 코드(예: "005930")
 * - side        : "BUY" / "SELL"
 * - orderKind   : "LIMIT" / "MARKET" (필요 시 CANCEL 등 확장 가능)
 * - price       : 원화 정수(정수 KRW). LIMIT일 때만 사용(시장가는 가드 가격으로 다른 경로에서 처리)
 * - quantity    : 주문 수량(소수 허용)
 * - createdAt   : 생성 시각(문자열 그대로 보관, 상위 레이어에서 파싱/검증)
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class InboundOrderMessage {
    private String brokerageId;
    private String orderId;
    private String ticker;     // 예: "005930"
    private String side;       // "BUY" / "SELL"
    private String orderKind;  // "LIMIT" / "MARKET"
    private long price;      // 원화 정수. LIMIT일 때만 사용
    private BigDecimal quantity;
    private String createdAt;  // 문자열이면 일단 그대로 둠
}