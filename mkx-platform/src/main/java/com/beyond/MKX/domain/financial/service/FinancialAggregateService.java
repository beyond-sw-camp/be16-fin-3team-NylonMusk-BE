package com.beyond.MKX.domain.financial.service;

import com.beyond.MKX.domain.financial.dto.*;
import com.beyond.MKX.domain.financial.entity.*;
import com.beyond.MKX.domain.financial.mapper.FinancialMapper;
import com.beyond.MKX.domain.financial.repository.*;
import com.beyond.MKX.domain.financial.util.FinancialRatiosAutoCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FinancialAggregateService {

    private final CompanyFinancialsRepository companyFinancialsRepository;
    private final CashFlowStatementRepository cashFlowStatementRepository;
    private final FinancialRatiosRepository financialRatiosRepository;
    private final FinancialRatiosAutoCalculator calculator;

    /**
     * 1. companyFinancials, cashFlowStatements, financialRatios 각각 null 체크
     * 2. 각 리스트를 엔티티로 변환(FinancialMapper::toEntity)
     * 3. DB 존재 여부 확인 후 있으면 update, 없으면 insert
     */
    @Transactional
    public void saveFinancialBundle(FinancialBundleReqDto requestDto) {

        if (requestDto.companyFinancials() != null) {
            requestDto.companyFinancials().stream()
                    .map(FinancialMapper::toEntity)
                    .forEach(cf -> {
                        // totalEquity 자동 보정
                        autofillEquity(cf);
                        upsertCompanyFinancials(cf);
                        upsertComputedRatiosFromFinancials(cf);
                    });
        }

        if (requestDto.cashFlowStatements() != null) {
            requestDto.cashFlowStatements().stream()
                    .map(FinancialMapper::toEntity)
                    .forEach(this::upsertCashFlowStatements);
        }

        if (requestDto.financialRatios() != null) {
            requestDto.financialRatios().stream()
                    .map(FinancialMapper::toEntity)
                    .forEach(this::upsertFinancialRatios);
        }
    }

    // Alias for new naming in upload service
    @Transactional
    public void saveBundle(FinancialBundleReqDto dto) {
        saveFinancialBundle(dto);
    }

    //손익계산서(CompanyFinancials) 업서트 처리
    private void upsertCompanyFinancials(CompanyFinancials newRecord) {
        Optional<CompanyFinancials> existingRecord =
                companyFinancialsRepository.findByStockIdAndFiscalYearAndFiscalQuarter(
                        newRecord.getStockId(),
                        newRecord.getFiscalYear(),
                        newRecord.getFiscalQuarter()
                );

        if (existingRecord.isPresent()) {
            // 이미 존재 → update (엔티티 내부의 updateFrom 메서드 호출)
            CompanyFinancials existing = existingRecord.get();
            // 존재하지 않음 → 새로 insert
            existing.updateFrom(newRecord);
        } else {
            companyFinancialsRepository.save(newRecord);
        }
    }

    // 현금흐름표(CashFlowStatement) 업서트 처리
    private void upsertCashFlowStatements(CashFlowStatement newRecord) {
        Optional<CashFlowStatement> existingRecord =
                cashFlowStatementRepository.findByStockIdAndFiscalYearAndFiscalQuarter(
                        newRecord.getStockId(),
                        newRecord.getFiscalYear(),
                        newRecord.getFiscalQuarter()
                );

        if (existingRecord.isPresent()) {
            CashFlowStatement existing = existingRecord.get();
            existing.updateFrom(newRecord);
        } else {
            cashFlowStatementRepository.save(newRecord);
        }
    }

    // 재무비율(FinancialRatios) 업서트 처리
    private void upsertFinancialRatios(FinancialRatios newRecord) {
        Optional<FinancialRatios> existingRecord =
                financialRatiosRepository.findByStockIdAndFiscalYearAndFiscalQuarter(
                        newRecord.getStockId(),
                        newRecord.getFiscalYear(),
                        newRecord.getFiscalQuarter()
                );

        if (existingRecord.isPresent()) {
            FinancialRatios existing = existingRecord.get();
            existing.updateFrom(newRecord);
        } else {
            financialRatiosRepository.save(newRecord);
        }
    }

    private void upsertComputedRatiosFromFinancials(CompanyFinancials cf) {
        BigDecimal roe = calculator.calculateROE(cf);
        BigDecimal roa = calculator.calculateROA(cf);
        BigDecimal debt = calculator.calculateDebtRatio(cf);
        BigDecimal opMargin = calculator.calculateOperatingMargin(cf);
        BigDecimal netMargin = calculator.calculateNetMargin(cf);
        BigDecimal currRatio = calculator.calculateCurrentRatio(cf);
        BigDecimal intCov = calculator.calculateInterestCoverage(cf);

        Optional<FinancialRatios> existingRecord =
                financialRatiosRepository.findByStockIdAndFiscalYearAndFiscalQuarter(
                        cf.getStockId(), cf.getFiscalYear(), cf.getFiscalQuarter()
                );

        if (existingRecord.isPresent()) {
            FinancialRatios existing = existingRecord.get();
            existing.setRoe(roe);
            existing.setRoa(roa);
            existing.setDebtRatio(debt);
            existing.setOperatingMargin(opMargin);
            existing.setNetMargin(netMargin);
            existing.setCurrentRatio(currRatio);
            existing.setInterestCoverage(intCov);
        } else {
            FinancialRatios created = FinancialRatios.builder()
                    .stockId(cf.getStockId())
                    .fiscalYear(cf.getFiscalYear())
                    .fiscalQuarter(cf.getFiscalQuarter())
                    .roe(roe)
                    .roa(roa)
                    .debtRatio(debt)
                    .operatingMargin(opMargin)
                    .netMargin(netMargin)
                    .currentRatio(currRatio)
                    .interestCoverage(intCov)
                    .build();
            financialRatiosRepository.save(created);
        }
    }

    private void autofillEquity(CompanyFinancials cf) {
        if (cf.getTotalEquity() == null && cf.getTotalAssets() != null && cf.getTotalLiabilities() != null) {
            long equity = cf.getTotalAssets() - cf.getTotalLiabilities();
            cf.setTotalEquity(equity);
        }
    }

    /**  단일 종목 전체 재무데이터 조회  */
    @Transactional(readOnly = true)
    public FinancialBundleResDto getBundleByTicker(String ticker, String range) {

        List<CompanyFinancials> financialList =
                companyFinancialsRepository.findAllByTickerOrderByDesc(ticker);
        List<CashFlowStatement> cashFlowList =
                cashFlowStatementRepository.findAllByTickerOrderByDesc(ticker);
        List<FinancialRatios> ratiosList =
                financialRatiosRepository.findAllByTickerOrderByDesc(ticker);

        List<CompanyFinancialsResDto> filteredFinancials =
                filterByRange(financialList, range).stream()
                        .map(FinancialMapper::toRes)
                        .toList();

        List<CashFlowStatementResDto> filteredCashFlows =
                filterByRange(cashFlowList, range).stream()
                        .map(FinancialMapper::toRes)
                        .toList();

        List<FinancialRatiosResDto> filteredRatios =
                filterByRange(ratiosList, range).stream()
                        .map(FinancialMapper::toRes)
                        .toList();

        return FinancialBundleResDto.builder()
                .companyFinancials(filteredFinancials)
                .cashFlowStatements(filteredCashFlows)
                .financialRatios(filteredRatios)
                .build();
    }

    /**
     * range 값에 따라 연간/분기별 데이터를 필터링
     *
     * @param list  원본 데이터 리스트
     * @param range "annual" 또는 "quarterly"
     * @return 필터링된 리스트
     */
    private <T> List<T> filterByRange(List<T> list, String range) {
        if (range == null || range.isBlank() || "quarterly".equalsIgnoreCase(range)) {
            return list;
        }
        if ("annual".equalsIgnoreCase(range)) {
            return list.stream()
                    .filter(this::isAnnualRecord)
                    .toList();
        }
        return list;
    }

    /**
     * 주어진 객체가 연간 데이터인지 판단
     * - 분기 값이 null 또는 4(4분기)면 연간으로 간주
     */
    private boolean isAnnualRecord(Object record) {
        Integer fiscalQuarter = null;

        if (record instanceof CompanyFinancials financials) {
            fiscalQuarter = financials.getFiscalQuarter();
        } else if (record instanceof CashFlowStatement cashFlow) {
            fiscalQuarter = cashFlow.getFiscalQuarter();
        } else if (record instanceof FinancialRatios ratios) {
            fiscalQuarter = ratios.getFiscalQuarter();
        }

        // 정책: 분기 값이 null 또는 4(4분기)인 경우 연간 데이터로 간주
        return fiscalQuarter == null || fiscalQuarter == 4;
    }
}
