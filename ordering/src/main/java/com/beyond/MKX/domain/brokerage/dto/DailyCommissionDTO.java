package com.beyond.MKX.domain.brokerage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 일별 수수료 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyCommissionDTO {
    private LocalDate date;
    private Long totalCommission;    // 총 수수료
    private Long buyCommission;      // 매수 수수료
    private Long sellCommission;     // 매도 수수료
    private Integer tradeCount;      // 거래 건수
    private Integer activeTraders;   // 활성 거래자 수
}
