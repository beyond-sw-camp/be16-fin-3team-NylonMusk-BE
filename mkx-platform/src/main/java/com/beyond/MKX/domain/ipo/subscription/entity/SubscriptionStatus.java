package com.beyond.MKX.domain.ipo.subscription.entity;

public enum SubscriptionStatus {
    APPLIED,    // 신청 완료
    PAID,       // 납입 완료 (증거금 예치)
    CANCELLED,  // 청약 취소
    REFUNDED,   // 환불 완료
    ALLOCATED,  // 배정 완료
    SETTLED     // ✅ 정산 완료 (모든 금전 이동 완료)
}
