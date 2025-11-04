package com.beyond.MKX.domain.financial.mapper;

import com.beyond.MKX.domain.financial.dto.*;
import com.beyond.MKX.domain.financial.entity.*;

public final class FinancialMapper {
    private FinancialMapper(){}

    // ---- Entity -> ResDto
    public static CompanyFinancialsResDto toRes(CompanyFinancials e){
        return new CompanyFinancialsResDto(
                e.getId(), e.getStockId(), e.getFiscalYear(), e.getFiscalQuarter(),
                e.getRevenue(), e.getOperatingIncome(), e.getNetIncome(),
                e.getEps(), e.getTotalAssets(), e.getTotalLiabilities(), e.getTotalEquity(),
                e.getCurrentAssets(), e.getCurrentLiabilities(), e.getInterestExpense(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
    public static CashFlowStatementResDto toRes(CashFlowStatement e){
        return new CashFlowStatementResDto(
                e.getId(), e.getStockId(), e.getFiscalYear(), e.getFiscalQuarter(),
                e.getOperatingCashFlow(), e.getInvestingCashFlow(),
                e.getFinancingCashFlow(), e.getFreeCashFlow(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
    public static FinancialRatiosResDto toRes(FinancialRatios e){
        return new FinancialRatiosResDto(
                e.getId(), e.getStockId(), e.getFiscalYear(), e.getFiscalQuarter(),
                // PER, PBR, PSR은 StockPriceRatios에서만 조회 (현재가 기반 비율)
                e.getBps(),
                e.getOperatingMargin(), e.getNetMargin(),
                e.getDebtRatio(), e.getCurrentRatio(), e.getInterestCoverage(),
                e.getRoa(), e.getRoe(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    // ---- ReqDto -> Entity
    public static CompanyFinancials toEntity(CompanyFinancialsReqDto d){
        return CompanyFinancials.builder()
                .stockId(d.stockId())
                .fiscalYear(d.fiscalYear())
                .fiscalQuarter(d.fiscalQuarter())
                .revenue(d.revenue())
                .operatingIncome(d.operatingIncome())
                .netIncome(d.netIncome())
                .eps(d.eps())
                .totalAssets(d.totalAssets())
                .totalLiabilities(d.totalLiabilities())
                .totalEquity(d.totalEquity())
                .currentAssets(d.currentAssets())
                .currentLiabilities(d.currentLiabilities())
                .interestExpense(d.interestExpense())
                .build();
    }
    public static CashFlowStatement toEntity(CashFlowStatementReqDto d){
        return CashFlowStatement.builder()
                .stockId(d.stockId())
                .fiscalYear(d.fiscalYear())
                .fiscalQuarter(d.fiscalQuarter())
                .operatingCashFlow(d.operatingCashFlow())
                .investingCashFlow(d.investingCashFlow())
                .financingCashFlow(d.financingCashFlow())
                .freeCashFlow(d.freeCashFlow())
                .build();
    }
    public static FinancialRatios toEntity(FinancialRatiosReqDto d){
        return FinancialRatios.builder()
                .stockId(d.stockId())
                .fiscalYear(d.fiscalYear())
                .fiscalQuarter(d.fiscalQuarter())
                // PER, PBR, PSR은 StockPriceRatios에만 저장 (현재가 기반 비율)
                .bps(d.bps())
                .operatingMargin(d.operatingMargin())
                .netMargin(d.netMargin())
                .debtRatio(d.debtRatio())
                .currentRatio(d.currentRatio())
                .interestCoverage(d.interestCoverage())
                .roa(d.roa())
                .roe(d.roe())
                .build();
    }
}
