package com.beyond.MKX.domain.financial.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisclosureEarningsValidationDto {
    private int fiscalYear;
    private Integer fiscalQuarter;
    private Long revenue;
    private Long operatingIncome;
    private Long netIncome;
    private BigDecimal eps;
    private Long totalAssets;
    private Long totalLiabilities;
    private Long currentAssets;
    private Long currentLiabilities;
    private Long interestExpense;
}
