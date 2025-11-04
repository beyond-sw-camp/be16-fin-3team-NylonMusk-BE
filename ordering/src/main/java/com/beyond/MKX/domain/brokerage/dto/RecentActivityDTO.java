package com.beyond.MKX.domain.brokerage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 증권사 대시보드용 최근 활동 DTO
 * - order_log와 ledger를 통합하여 최근 활동 목록을 제공
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDTO {
    private UUID id;
    private String type; // "ORDER" 또는 "LEDGER"
    private String activityType; // 주문/거래 유형
    private String activityTypeKorean; // 주문/거래 유형 한글명
    private String ticker; // 종목 티커
    private String stockName; // 종목명
    private Long quantity; // 수량
    private Long amount; // 금액
    private String accountNumber; // 계좌번호
    private String memberName; // 회원명
    private LocalDateTime createdAt; // 생성 일시
}

