package com.beyond.MKX.domain.financial.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FinancialRatiosReqDto(
        UUID stockId,
        int fiscalYear,
        Integer fiscalQuarter,
        BigDecimal per,
        BigDecimal pbr,
        BigDecimal bps,  // 주당순자산가치 추가
        BigDecimal operatingMargin,
        BigDecimal netMargin,
        BigDecimal debtRatio,
        BigDecimal currentRatio,
        BigDecimal interestCoverage,
        BigDecimal roa,
        BigDecimal roe
) {}
