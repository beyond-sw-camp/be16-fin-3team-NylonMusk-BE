package com.beyond.MKX.domain.brokerage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시간대별 수수료 추이 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HourlyCommissionTrendDTO {
    private Integer hour;                // 시간대 (0-23)
    private Long totalCommission;        // 해당 시간대 총 수수료
    private Long buyCommission;          // 매수 수수료
    private Long sellCommission;         // 매도 수수료
    private Integer tradeCount;          // 거래 건수
    private Integer activeTraders;       // 활성 거래자 수
}
