package com.beyond.MKX.domain.brokerage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 수수료 차트 데이터 DTO
 * - 7일, 30일, 90일, 365일 기간별 차트 데이터 제공
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionChartDTO {
    private String period;                      // "7days", "30days", "90days", "365days"
    private LocalDate startDate;                // 시작일
    private LocalDate endDate;                  // 종료일 (조회한 날짜)
    private Long totalCommission;               // 전체 기간 총 수수료
    private Integer dataPointCount;             // 차트 막대 개수
    private List<ChartDataPoint> dataPoints;    // 차트 데이터 포인트 리스트

    /**
     * 차트의 각 막대 데이터
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataPoint {
        private String label;                   // 차트 라벨 (날짜 또는 기간)
        private LocalDate startDate;            // 해당 구간 시작일
        private LocalDate endDate;              // 해당 구간 종료일
        private Long totalCommission;           // 해당 구간 총 수수료
        private Long buyCommission;             // 해당 구간 매수 수수료
        private Long sellCommission;            // 해당 구간 매도 수수료
        private Integer tradeCount;             // 해당 구간 거래 건수
        private Integer activeTraders;          // 해당 구간 활성 거래자 수
    }
}
