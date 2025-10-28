package com.beyond.MKX.domain.orderbook.dto.enhanced;

import lombok.*;

import java.math.BigDecimal;

/**
 * 호가창 통계 정보 DTO
 * 
 * 호가창의 분석 지표 및 통계 정보
 * - 중간호가, 스프레드
 * - 체결강도
 * - 총 매수/매도 잔량
 * - 호가 깊이
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class OrderBookStatistics {
    
    // ========== 가격 관련 통계 ==========
    
    /**
     * 중간호가
     * 계산식: (최우선매수호가 + 최우선매도호가) / 2
     * 
     * 시장의 현재 균형가를 나타냄
     */
    private Long midPrice;
    
    /**
     * 스프레드 (절대값)
     * 계산식: 최우선매도호가 - 최우선매수호가
     */
    private Long spreadAmount;
    
    /**
     * 스프레드 비율 (%)
     * 계산식: spreadAmount / midPrice * 100
     * 
     * 유동성 지표: 스프레드가 작을수록 유동성이 높음
     */
    private BigDecimal spreadPercent;
    
    // ========== 체결강도 ==========
    
    /**
     * 체결강도 (%)
     * 계산식: (매수체결량 / 매도체결량) * 100
     * 
     * 해석:
     * - 100 이상: 매수세 우위
     * - 100 이하: 매도세 우위
     * - 최근 5분간 데이터 기준
     */
    private BigDecimal executionStrength;
    
    /**
     * 최근 5분간 매수 체결량
     */
    private BigDecimal recentBuyVolume;
    
    /**
     * 최근 5분간 매도 체결량
     */
    private BigDecimal recentSellVolume;
    
    // ========== 호가 잔량 통계 ==========
    
    /**
     * 총 매수 호가 잔량
     * 호가창에 있는 모든 매수 주문의 합계
     */
    private BigDecimal totalBidVolume;
    
    /**
     * 총 매도 호가 잔량
     * 호가창에 있는 모든 매도 주문의 합계
     */
    private BigDecimal totalAskVolume;
    
    /**
     * 매수 우위 비율 (%)
     * 계산식: totalBidVolume / (totalBidVolume + totalAskVolume) * 100
     * 
     * 호가창 기준 매수/매도 세력 균형 지표
     */
    private BigDecimal bidRatio;
    
    /**
     * 매도 우위 비율 (%)
     * 계산식: totalAskVolume / (totalBidVolume + totalAskVolume) * 100
     */
    private BigDecimal askRatio;
    
    // ========== 호가 깊이 ==========
    
    /**
     * 매수 호가 깊이
     * 호가창에 표시된 매수 호가의 개수
     */
    private Integer bidDepth;
    
    /**
     * 매도 호가 깊이
     * 호가창에 표시된 매도 호가의 개수
     */
    private Integer askDepth;
    
    /**
     * 총 호가 개수
     */
    private Integer totalDepth;
    
    // ========== 최우선 호가 정보 ==========
    
    /**
     * 최우선 매수호가 (가장 높은 매수 가격)
     */
    private Long bestBidPrice;
    
    /**
     * 최우선 매수호가 수량
     */
    private BigDecimal bestBidQuantity;
    
    /**
     * 최우선 매도호가 (가장 낮은 매도 가격)
     */
    private Long bestAskPrice;
    
    /**
     * 최우선 매도호가 수량
     */
    private BigDecimal bestAskQuantity;
    
    // ========== 타임스탬프 ==========
    
    /**
     * 통계 계산 시각
     */
    private Long timestamp;
}
