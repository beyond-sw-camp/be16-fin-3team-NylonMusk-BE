package com.beyond.MKX.domain.organization.entity;

/** 회사 상태 */
public enum OrganizationStatus {
    ACTIVE,    // 정상
    PENDING,   // 승인 대기/심사 중
    DELISTED   // 상장폐지/비활성
}