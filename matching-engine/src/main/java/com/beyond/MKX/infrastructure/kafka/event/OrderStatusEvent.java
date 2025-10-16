package com.beyond.MKX.infrastructure.kafka.event;

import lombok.*;

import java.math.BigDecimal;

/**
 * 주문 상태(Order Status) 이벤트 DTO.
 *
 * 목적
 * - 매칭 엔진이 주문의 상태 변화를 카프카 "order-status" 토픽으로 알릴 때 사용하는 페이로드 모델
 * - 컨슈머(모니터링/알림/주문현황 UI)가 실시간 상태를 구독하기 위한 표준 스키마
 *
 * 필드 설명
 * - orderId         : 주문 식별자
 * - status          : 상태 코드 (예: NEW_ACCEPTED, MARKET_PARTIAL, MARKET_FILLED, WAITING, CANCEL_OK, …)
 * - ticker / side   : 선택적 메타데이터(필요 시만 포함)
 * - price           : 대표 가격(기본은 VWAP 등으로 사용; 발신 측 정책에 따름)
 * - remaining       : 남은 적재 수량(부분 체결 시 유의)
 * - timestamp       : 이벤트 생성 시각(epoch millis)
 * - avgFillPrice    : 체결 평균가(VWAP)
 * - lastFillPrice   : 마지막 체결가
 * - limitPrice      : 지정가(또는 시장가 가드 가격)
 * - filledQuantity  : 누적 체결 수량
 * - notional        : 총 체결 금액(원화 정수, ∑(체결가*체결수량))
 */
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
    private long price;      // optional
    private BigDecimal remaining;  // optional
    private long timestamp;
    private long avgFillPrice;
    private long lastFillPrice;
    private long limitPrice;
    private BigDecimal filledQuantity;
    private long notional;
}
