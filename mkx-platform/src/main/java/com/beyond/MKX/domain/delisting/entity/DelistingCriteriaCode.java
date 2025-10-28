package com.beyond.MKX.domain.delisting.entity;

import lombok.Getter;

/**
 * 상장폐지 기준 코드 Enum
 * 위키백과 상장폐지 요건을 기반으로 정의
 */
@Getter
public enum DelistingCriteriaCode {
    
    // ====== 재무 기준 ======
    LOW_REVENUE_2Y("LOW_REVENUE_2Y", "매출액 50억원 미만 2년 연속", CriteriaType.FINANCIAL),
    EQUITY_EROSION_100_PERCENT("EQUITY_EROSION_100_PERCENT", "연말 자본금 전액 잠식", CriteriaType.FINANCIAL),
    CONSECUTIVE_LOSS_3Q("CONSECUTIVE_LOSS_3Q", "연속 3분기 적자", CriteriaType.FINANCIAL),
    CONSECUTIVE_LOSS_2Y("CONSECUTIVE_LOSS_2Y", "연속 2년 적자", CriteriaType.FINANCIAL),
    
    // ====== 거래 기준 ======
    LOW_TRADING_VOLUME_2Q("LOW_TRADING_VOLUME_2Q", "분기 월 평균 거래량이 유동주식 수의 1% 미만 2분기 연속", CriteriaType.TRADING),
    LOW_STOCK_PRICE_30D("LOW_STOCK_PRICE_30D", "30일 평균 주가 1000원 미만", CriteriaType.TRADING),
    LOW_MARKET_CAP_CONTINUOUS("LOW_MARKET_CAP_CONTINUOUS", "관리종목 지정 후 시가총액 부족 상태 지속", CriteriaType.TRADING),
    LOW_MINORITY_SHAREHOLDERS("LOW_MINORITY_SHAREHOLDERS", "소액주주 수 200인 미만(또는 지분 20% 미만) 2년 연속", CriteriaType.TRADING),
    
    // ====== 법규 기준 ======
    AUDIT_OPINION_ADVERSE("AUDIT_OPINION_ADVERSE", "감사 의견 부적정", CriteriaType.REGULATORY),
    AUDIT_OPINION_QUALIFIED("AUDIT_OPINION_QUALIFIED", "감사 의견 한정", CriteriaType.REGULATORY),
    AUDIT_OPINION_DISCLAIMER("AUDIT_OPINION_DISCLAIMER", "감사 의견 거절", CriteriaType.REGULATORY),
    REPORT_DELAY("REPORT_DELAY", "분기별 사업보고서 제출 기한 후 10일내 미제출", CriteriaType.REGULATORY),
    FINAL_BANKRUPTCY("FINAL_BANKRUPTCY", "최종 부도 발생", CriteriaType.REGULATORY),
    EMBEZZLEMENT("EMBEZZLEMENT", "횡령 발생", CriteriaType.REGULATORY),
    BREACH_OF_TRUST("BREACH_OF_TRUST", "배임 발생", CriteriaType.REGULATORY);
    
    private final String code;
    private final String description;
    private final CriteriaType criteriaType;
    
    DelistingCriteriaCode(String code, String description, CriteriaType criteriaType) {
        this.code = code;
        this.description = description;
        this.criteriaType = criteriaType;
    }
    
    /**
     * 코드로 Enum 찾기
     */
    public static DelistingCriteriaCode fromCode(String code) {
        for (DelistingCriteriaCode criteriaCode : values()) {
            if (criteriaCode.getCode().equals(code)) {
                return criteriaCode;
            }
        }
        throw new IllegalArgumentException("Unknown criteria code: " + code);
    }
    
    /**
     * 재무 기준만 필터링
     */
    public static DelistingCriteriaCode[] getFinancialCriteria() {
        return java.util.Arrays.stream(values())
                .filter(c -> c.getCriteriaType() == CriteriaType.FINANCIAL)
                .toArray(DelistingCriteriaCode[]::new);
    }
    
    /**
     * 거래 기준만 필터링
     */
    public static DelistingCriteriaCode[] getTradingCriteria() {
        return java.util.Arrays.stream(values())
                .filter(c -> c.getCriteriaType() == CriteriaType.TRADING)
                .toArray(DelistingCriteriaCode[]::new);
    }
    
    /**
     * 법규 기준만 필터링
     */
    public static DelistingCriteriaCode[] getRegulatoryCriteria() {
        return java.util.Arrays.stream(values())
                .filter(c -> c.getCriteriaType() == CriteriaType.REGULATORY)
                .toArray(DelistingCriteriaCode[]::new);
    }
}
