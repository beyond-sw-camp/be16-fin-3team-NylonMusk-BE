package com.beyond.MKX.domain.account.corporation.entity;

/**
 * 모든 계좌의 공통 상태 (승인 절차가 있는 계좌에만 사용)
 */
public enum AccountStatus {
    PENDING,     // 등록 대기
    APPROVED,    // 승인 완료
    REJECTED,    // 반려됨
    SUSPENDED    // 거래 일시 정지
}
