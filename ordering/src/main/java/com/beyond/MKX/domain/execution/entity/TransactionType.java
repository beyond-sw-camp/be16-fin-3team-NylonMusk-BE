package com.beyond.MKX.domain.execution.entity;

/**
 * 거래 유형을 나타내는 enum
 */
public enum TransactionType {
    BUY("매수"),
    SELL("매도"),
    ORDER_REFUND("주문 환불"),
    DELISTING_REFUND("상장폐지 환불"),
    IPO_PAID("공모 참가"),
    IPO_REFUND("공모 환불"),
    IPO_ADDITIONAL("공모 추가 납입"),
    DEPOSIT("입금"),
    WITHDRAWAL("출금"),
    TRANSFER("계좌이체");

    private final String koreanName;

    TransactionType(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {
        return koreanName;
    }
}
