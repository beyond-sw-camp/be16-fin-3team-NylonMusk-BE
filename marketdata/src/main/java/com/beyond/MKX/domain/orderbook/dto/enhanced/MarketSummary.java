package com.beyond.MKX.domain.orderbook.dto.enhanced;

import lombok.*;

import java.math.BigDecimal;

/**
 * 시장 요약 정보 DTO
 * 
 * 호가창 상단에 표시되는 시장 전반 정보
 * - 52주 최고/최저가
 * - 상한가/하한가 (30% 제한)
 * - 당일 가격 정보 (시가, 고가, 저가)
 * - 거래량 및 등락 정보
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MarketSummary {
    
    // ========== 52주 범위 ==========
    
    /**
     * 52주 최고가
     * InfluxDB에서 계산된 값 (주기적 갱신)
     */
    private Long week52High;
    
    /**
     * 52주 최저가
     * InfluxDB에서 계산된 값 (주기적 갱신)
     */
    private Long week52Low;
    
    // ========== 상한가/하한가 제한 ==========
    
    /**
     * 상한가 (기준가 대비 +30%)
     * 계산식: basePrice * 1.30
     */
    private Long upperLimit;
    
    /**
     * 하한가 (기준가 대비 -30%)
     * 계산식: basePrice * 0.70
     */
    private Long lowerLimit;
    
    /**
     * 기준가 (전일 종가 또는 당일 시가)
     * 상한가/하한가 계산의 기준이 되는 가격
     */
    private Long basePrice;
    
    // ========== 당일 가격 정보 ==========
    
    /**
     * 시가 (당일 첫 체결가)
     */
    private Long openPrice;
    
    /**
     * 고가 (당일 최고 체결가)
     */
    private Long highPrice;
    
    /**
     * 저가 (당일 최저 체결가)
     */
    private Long lowPrice;
    
    /**
     * 현재가 (최근 체결가)
     */
    private Long currentPrice;
    
    // ========== 거래량 정보 ==========
    
    /**
     * 당일 누적 거래량
     */
    private BigDecimal volume;
    
    /**
     * 전일 거래량 (비교용)
     */
    private BigDecimal prevVolume;
    
    /**
     * 거래량 변화율 (%)
     * 계산식: (volume - prevVolume) / prevVolume * 100
     */
    private BigDecimal volumeChangeRate;
    
    // ========== 등락 정보 ==========
    
    /**
     * 전일 종가
     */
    private Long prevClose;
    
    /**
     * 전일대비 등락액
     * 계산식: currentPrice - prevClose (또는 basePrice)
     */
    private Long changeFromYesterday;
    
    /**
     * 전일대비 등락률 (%)
     * 계산식: changeFromYesterday / prevClose * 100
     */
    private BigDecimal changeRate;
    
    /**
     * 등락 방향
     * RISE: 상승
     * FALL: 하락
     * STEADY: 보합
     */
    private String trend;
    
    // ========== 타임스탬프 ==========
    
    /**
     * 마지막 업데이트 시각
     */
    private Long timestamp;
}
