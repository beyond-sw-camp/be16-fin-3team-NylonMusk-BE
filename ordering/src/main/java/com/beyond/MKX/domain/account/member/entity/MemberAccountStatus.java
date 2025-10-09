package com.beyond.MKX.domain.account.member.entity;

/**
 * 유저(Member) 계좌 상태
 * - 거래 가능 여부를 나타냄
 */
public enum MemberAccountStatus {
    ACTIVE,       // 정상 거래 가능
    SUSPENDED,    // 거래 정지 (위반, 보류, AML 등)
    DELETED       // 삭제 (폐쇄된 계좌)
}