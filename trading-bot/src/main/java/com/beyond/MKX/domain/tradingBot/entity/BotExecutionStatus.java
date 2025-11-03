package com.beyond.MKX.domain.tradingBot.entity;

/**
 * 봇 실행 상태 enum
 * 마지막 주문 실행 결과를 나타냄
 */
public enum BotExecutionStatus {
    SUCCESS,                    // 주문 성공
    INSUFFICIENT_BALANCE,       // 잔고 부족
    INSUFFICIENT_STOCK,         // 보유주식 부족
    ADJUSTED_QUANTITY,          // 수량 조정 후 주문 성공
    ERROR,                      // 기타 에러 (네트워크, API 에러 등)
    SKIPPED                     // 스킵 (최소 주문 불가능)
}

