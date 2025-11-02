package com.beyond.MKX.domain.brokerage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 증권사 대시보드 통계 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerageStatsDTO {
    private Long totalCustomers; // 총 고객 수 (해당 증권사를 사용하는 총 회원 수)
    private Long activeAccounts; // 활성 계좌 수 (APPROVED/ACTIVE 상태의 계좌 수)
    private Long onlineCustomers; // 오늘 주문 고객 수 (오늘 00:00 ~ 23:59:59 안에 주문을 넣은 회원 계좌 수)
    private Long dailyVolume; // 일일 거래량 (오늘 날짜의 거래 금액 합계)
    private Long monthlyRevenue; // 월간 수익 (이번 달의 수수료 합계)
    private Long buyCommission; // 매수 수수료 (이번 달)
    private Long sellCommission; // 매도 수수료 (이번 달)
    private Double volumeChangePercent; // 거래량 변화율 (전날 대비)
    private Double revenueChangePercent; // 수익 변화율 (전월 대비)
}
