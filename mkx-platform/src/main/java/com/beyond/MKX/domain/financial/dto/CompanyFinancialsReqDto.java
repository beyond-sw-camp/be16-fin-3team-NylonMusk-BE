package com.beyond.MKX.domain.financial.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CompanyFinancialsReqDto(
        UUID stockId,
        int fiscalYear,
        Integer fiscalQuarter,
        Long revenue,
        Long operatingIncome,
        Long netIncome,
        BigDecimal eps,
        Long totalAssets,
        Long totalLiabilities,
        Long totalEquity,
        Long currentAssets,
        Long currentLiabilities,
        Long interestExpense
) {}
