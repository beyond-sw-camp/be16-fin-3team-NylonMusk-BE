package com.beyond.MKX.domain.delisting.entity;

public enum DelistingReason {
    FINANCIAL_DISTRESS,     // 재무 악화
    LOW_TRADING_VOLUME,     // 거래량 부족
    REGULATORY_VIOLATION,   // 법규 위반
    REPORT_DELAY,           // 제출 지연
    BANKRUPTCY,             // 부도
    MERGER_ACQUISITION,     // 합병/인수
    VOLUNTARY_DELISTING     // 자진 상장폐지
}
