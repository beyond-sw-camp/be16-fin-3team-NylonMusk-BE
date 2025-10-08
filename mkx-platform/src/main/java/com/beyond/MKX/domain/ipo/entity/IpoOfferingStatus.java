package com.beyond.MKX.domain.ipo.entity;

public enum IpoOfferingStatus {
    DRAFT,     // 초안 상태 (아직 관리자 승인 전)
    OPEN,      // 청약 진행 중
    CLOSED,    // 청약 마감 (배정 전)
    SETTLED,   // 배정/환불까지 완료
    CANCELLED  // 취소
}
