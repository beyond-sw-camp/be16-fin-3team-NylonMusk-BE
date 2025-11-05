package com.beyond.MKX.domain.brokerage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 기간별 수수료 분석 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodCommissionDTO {
    private Integer periodDays;          // 기간 (7, 30, 90, 365)
    private Long totalCommission;        // 총 수수료
    private Long buyCommission;          // 매수 수수료
    private Long sellCommission;         // 매도 수수료
    private Double dailyAverage;         // 일평균 수수료
    private Double changePercent;        // 이전 기간 대비 변화율
    private List<DailyCommissionDTO> dailyBreakdown; // 일별 상세
}
