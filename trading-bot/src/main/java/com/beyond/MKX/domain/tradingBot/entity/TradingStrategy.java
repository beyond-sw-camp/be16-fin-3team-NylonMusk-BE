package com.beyond.MKX.domain.tradingBot.entity;

/**
 * 트렌드 전략 enum
 * 주문 가격 계산 시 사용하는 트렌드 전략
 */
public enum TradingStrategy {
    SIDEWAYS,      // 횡보 - 현재가 ±0.2% 범위 내
    DOWNWARD,      // 하락 - 현재가 -0.2% ~ -0.4% 범위
    UPWARD,        // 상승 - 현재가 +0.2% ~ +0.4% 범위
    SHARP_DOWN,    // 급하락 - 현재가 -0.4% ~ -0.6% 범위
    SHARP_UP       // 급상승 - 현재가 +0.4% ~ +0.6% 범위
}

