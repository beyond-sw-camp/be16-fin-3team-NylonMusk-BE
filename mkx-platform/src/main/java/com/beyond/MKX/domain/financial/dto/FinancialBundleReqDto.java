package com.beyond.MKX.domain.financial.dto;

import java.util.List;

public record FinancialBundleReqDto(
        List<CompanyFinancialsReqDto> companyFinancials,
        List<CashFlowStatementReqDto> cashFlowStatements,
        List<FinancialRatiosReqDto> financialRatios
) {}
