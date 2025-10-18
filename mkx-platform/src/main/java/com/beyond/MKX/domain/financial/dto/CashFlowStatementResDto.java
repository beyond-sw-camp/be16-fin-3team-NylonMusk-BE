package com.beyond.MKX.domain.financial.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CashFlowStatementResDto(
        UUID id,
        UUID stockId,
        int fiscalYear,
        Integer fiscalQuarter,
        Long operatingCashFlow,
        Long investingCashFlow,
        Long financingCashFlow,
        Long freeCashFlow,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
