package com.beyond.MKX.domain.financial.dto;

import java.util.UUID;

public record CashFlowStatementReqDto(
        UUID stockId,
        int fiscalYear,
        Integer fiscalQuarter,
        Long operatingCashFlow,
        Long investingCashFlow,
        Long financingCashFlow,
        Long freeCashFlow
) {}
