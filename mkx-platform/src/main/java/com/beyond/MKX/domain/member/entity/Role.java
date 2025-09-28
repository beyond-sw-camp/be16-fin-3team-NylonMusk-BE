package com.beyond.MKX.domain.member.entity;

/** 사용자 역할(권한) */
public enum Role {
    ADMIN,      // 거래소 관리자(전용 백오피스)
    CORPORATION,   // 기업 관리자(대표자)
    BROKERAGE,   // 거래 기업 관리자(대표자)
    BROKER,   // 브로커/증권사 관리자
}