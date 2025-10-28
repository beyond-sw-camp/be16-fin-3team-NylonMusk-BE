package com.beyond.MKX.domain.delisting.entity;

public enum SubmissionStatus {
    PENDING,              // 제출 대기
    SUBMITTED,            // 제출 완료
    UNDER_REVIEW,         // 검토 중
    APPROVED,             // 승인
    REJECTED,             // 반려
    OVERDUE              // 제출 지연
}
