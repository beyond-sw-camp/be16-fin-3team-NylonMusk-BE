package com.beyond.MKX.domain.financial.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FinancialRatiosResDto(
        UUID id,
        UUID stockId,
        int fiscalYear,
        Integer fiscalQuarter,
        BigDecimal per,
        BigDecimal pbr,
        BigDecimal operatingMargin,
        BigDecimal netMargin,
        BigDecimal debtRatio,
        BigDecimal currentRatio,
        BigDecimal interestCoverage,
        BigDecimal roa,
        BigDecimal roe,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
