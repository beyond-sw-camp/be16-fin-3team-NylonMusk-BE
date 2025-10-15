package com.beyond.MKX.domain.financial.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record FinancialBundleResDto(
        List<CompanyFinancialsResDto> companyFinancials,
        List<CashFlowStatementResDto> cashFlowStatements,
        List<FinancialRatiosResDto> financialRatios
) {}
