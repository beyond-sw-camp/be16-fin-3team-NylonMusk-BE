package com.beyond.MKX.domain.order.entity;

public enum OrderStatus {
    PENDING, // 대기
    PARTIALLY_FILLED, // 부분 체결
    FILLED, // 전량 체결
    CANCELED, // 취소
    REJECTED, // 거부
    EXPIRED // 만료 - 조건 주문 시 체결이 안되었을 때
}
