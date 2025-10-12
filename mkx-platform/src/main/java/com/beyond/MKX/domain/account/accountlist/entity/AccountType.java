package com.beyond.MKX.domain.account.accountlist.entity;

/**
 * 계좌의 종류 (거래소/증권사/기업/유저)
 */
public enum AccountType {
    EXCHANGE,      // 거래소 계좌
    BROKERAGE,     // 증권사 예치금 계좌
    CORPORATION,   // 기업 등록 계좌
    MEMBER         // 유저 계좌
}