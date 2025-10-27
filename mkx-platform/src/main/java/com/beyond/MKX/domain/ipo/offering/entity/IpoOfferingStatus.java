package com.beyond.MKX.domain.ipo.offering.entity;

public enum IpoOfferingStatus {
    SCHEDULED,          // 일정 확정: 개시 전 대기(현재 create 후 기본 상태로 사용중)
    OPEN,               // 청약 진행 중
    CLOSED,             // 청약 마감(집계/검증 가능)
    PRICE_FIXED,        // 확정 공모가 고시(밴드 내 확정). 배정 전제 충족 상태
    ALLOCATED,          // 배정 완료(배정 테이블 기록 완료)
    SETTLED,            // 환불/정산까지 완료(최종 종결)
    CANCELLED,           // 과정 중단(관리자 취소/에러 롤백 등)
    BOOK_BUILDING


}
