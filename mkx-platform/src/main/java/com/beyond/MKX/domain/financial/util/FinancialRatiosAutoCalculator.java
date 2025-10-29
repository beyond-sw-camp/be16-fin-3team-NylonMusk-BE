package com.beyond.MKX.domain.financial.util;

import com.beyond.MKX.domain.financial.entity.CompanyFinancials;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 재무비율 자동 계산 유틸리티
 * - 입력: CompanyFinancials(자산/부채/순이익 등 Long 값)
 * - 출력: 백분율(%) 스케일 2자리로 반올림한 BigDecimal
 *
 * 계산식(표준):
 *  - ROE(자기자본이익률)    = 순이익 / 자기자본 * 100
 *  - ROA(총자산이익률)      = 순이익 / 총자산   * 100
 *  - 부채비율               = 부채   / 자기자본 * 100
 *
 * 정책:
 *  - 자기자본(자산-부채) <= 0 이거나 분모가 0이면 0.00% 로 처리
 *  - 결과는 소수점 둘째 자리까지 HALF_UP 반올림
 */
@Component
public class FinancialRatiosAutoCalculator {

    /**
     * ROE(자기자본이익률) = 순이익 / (자산 - 부채) * 100
     * - 자기자본이 0 이하인 경우 0.00 반환
     */
    public BigDecimal calculateROE(CompanyFinancials cf) {
        double equity = toDouble(cf.getTotalAssets()) - toDouble(cf.getTotalLiabilities());
        if (equity <= 0) return BigDecimal.ZERO;
        return toPercentage(safeDivide(toDouble(cf.getNetIncome()), equity));
    }

    /**
     * ROA(총자산이익률) = 순이익 / 자산 * 100
     * - 자산이 0인 경우 0.00 반환
     */
    public BigDecimal calculateROA(CompanyFinancials cf) {
        return toPercentage(safeDivide(toDouble(cf.getNetIncome()), toDouble(cf.getTotalAssets())));
    }

    /**
     * 부채비율 = 부채 / (자산 - 부채) * 100
     * - 자기자본이 0 이하인 경우 0.00 반환
     */
    public BigDecimal calculateDebtRatio(CompanyFinancials cf) {
        double equity = toDouble(cf.getTotalAssets()) - toDouble(cf.getTotalLiabilities());
        if (equity <= 0) return BigDecimal.ZERO;
        return toPercentage(safeDivide(toDouble(cf.getTotalLiabilities()), equity));
    }

    /**
     * 영업이익률 = 영업이익 / 매출액 * 100
     * - 매출액이 0이면 0.00%
     */
    public BigDecimal calculateOperatingMargin(CompanyFinancials cf) {
        return toPercentage(safeDivide(toDouble(cf.getOperatingIncome()), toDouble(cf.getRevenue())));
    }

    /**
     * 순이익률 = 당기순이익 / 매출액 * 100
     * - 매출액이 0이면 0.00%
     */
    public BigDecimal calculateNetMargin(CompanyFinancials cf) {
        return toPercentage(safeDivide(toDouble(cf.getNetIncome()), toDouble(cf.getRevenue())));
    }

    /**
     * 유동비율 = 유동자산 / 유동부채 * 100
     * - 유동부채가 0이면 0.00%
     */
    public BigDecimal calculateCurrentRatio(CompanyFinancials cf) {
        return toPercentage(safeDivide(toDouble(cf.getCurrentAssets()), toDouble(cf.getCurrentLiabilities())));
    }

    /**
     * 이자보상배수 = 영업이익 / 이자비용 (배수)
     * - 이자비용이 0이면 0.00배
     */
    public BigDecimal calculateInterestCoverage(CompanyFinancials cf) {
        return toRatio(safeDivide(toDouble(cf.getOperatingIncome()), toDouble(cf.getInterestExpense())));
    }

    /**
     * BPS(주당순자산가치) = 자기자본 / 총발행주식수
     * - 총발행주식수가 0이면 0.00 반환
     */
    public BigDecimal calculateBPS(CompanyFinancials cf, Long totalSharesOutstanding) {
        if (totalSharesOutstanding == null || totalSharesOutstanding == 0) {
            return BigDecimal.ZERO;
        }
        double equity = toDouble(cf.getTotalEquity());
        return BigDecimal.valueOf(equity / totalSharesOutstanding)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /** Long → double null-safe 변환 */
    private static double toDouble(Long v) { return v == null ? 0.0 : v.doubleValue(); }

    /** 분모 0 방지 안전 나누기 */
    private static double safeDivide(double a, double b) { return (b == 0.0) ? 0.0 : a / b; }

    /** 배율을 %로 환산하고 소수 둘째 자리까지 반올림 */
    private static BigDecimal toPercentage(double ratio) {
        return BigDecimal.valueOf(ratio * 100.0).setScale(2, RoundingMode.HALF_UP);
    }

    /** 배수(%)가 아닌 지표용 스케일 2 고정 반올림 */
    private static BigDecimal toRatio(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
