package com.beyond.MKX.domain.brokerage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 일일 수수료 분석 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionAnalysisDTO {
    private LocalDate date;              // 조회 날짜
    private Long totalCommission;        // 총 수수료
    private Long buyCommission;          // 매수 수수료
    private Long sellCommission;         // 매도 수수료
    private Integer activeTraders;       // 활성 거래자 수
    private Double avgCommissionPerTrade; // 거래당 평균 수수료
}
