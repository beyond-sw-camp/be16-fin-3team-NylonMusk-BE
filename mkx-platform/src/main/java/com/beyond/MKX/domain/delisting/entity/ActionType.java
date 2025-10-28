package com.beyond.MKX.domain.delisting.entity;

public enum ActionType {
    CRITERIA_VIOLATION,    // 기준 위반
    STAGE_CHANGE,         // 단계 변경
    DELISTING_NOTICE,     // 상장폐지 예고
    DELISTING_EXECUTION,  // 상장폐지 실행
    DELISTING_CANCELLATION, // 상장폐지 취소
    COMPENSATION_FAILED   // 보상금 생성 실패
}
