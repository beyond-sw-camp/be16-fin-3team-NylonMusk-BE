package com.beyond.MKX.domain.member.entity;

/** 사용자 계정 상태 */
public enum UserStatus {
    ACTIVE,     // 정상
    SUSPENDED,  // 정지(로그인 실패/위반 등)
    DELETED     // 삭제/탈퇴
}