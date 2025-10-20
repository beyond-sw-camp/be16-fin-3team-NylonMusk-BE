package com.beyond.MKX.domain.financial.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyFinancialsResDto(
        UUID id,
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
        Long interestExpense,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
