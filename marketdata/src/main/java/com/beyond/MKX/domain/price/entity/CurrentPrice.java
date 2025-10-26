package com.beyond.MKX.domain.price.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 현재가 엔티티
 * 
 * 실시간 주가 정보를 담는 모델
 * 증권거래소의 체결 시스템과 동일하게 동작:
 * - 체결가가 현재가
 * - 호가창의 최우선 호가로 다음 체결가 예측
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CurrentPrice {
    
    private String ticker;              // 종목 코드
    
    // 가격 정보
    private long price;                 // 현재가 (최신 체결가)
    private long prevClose;             // 전일 종가
    private long open;                  // 시가 (당일 첫 체결가)
    private long high;                  // 고가 (당일 최고 체결가)
    private long low;                   // 저가 (당일 최저 체결가)
    
    // 등락 정보
    private long change;                // 전일대비 등락액 (현재가 - 전일종가)
    private BigDecimal changeRate;      // 전일대비 등락률 (%)
    
    // 거래량 정보
    private BigDecimal volume;          // 당일 누적 거래량
    private BigDecimal volumeChange;    // 거래량 변화율 (%)
    private BigDecimal prevVolume;      // 전일 거래량 (비교용)
    
    // 52주 가격 범위 (InfluxDB에서 계산)
    private long week52High;            // 52주 최고가 (0 = 데이터 없음)
    private long week52Low;             // 52주 최저가 (0 = 데이터 없음)
    
    // 체결강도 (매수체결량 / 매도체결량 * 100)
    private BigDecimal executionStrength; // 체결강도 (0 = 데이터 없음)
    
    // 호가 정보 (다음 체결 예상가)
    private Long bestBid;               // 최우선 매수호가 (가장 높은 매수 가격)
    private BigDecimal bestBidQuantity; // 최우선 매수호가 수량
    private Long bestAsk;               // 최우선 매도호가 (가장 낮은 매도 가격)
    private BigDecimal bestAskQuantity; // 최우선 매도호가 수량
    private Long spread;                // 스프레드 (매도호가 - 매수호가)
    
    // 시간 정보
    private Instant timestamp;          // 최종 업데이트 시각
    
    /**
     * 등락 방향
     * RISE: 상승
     * FALL: 하락
     * STEADY: 보합
     */
    @JsonIgnore
    public String getTrend() {
        if (change > 0) {
            return "RISE";
        } else if (change < 0) {
            return "FALL";
        } else {
            return "STEADY";
        }
    }
    
    /**
     * 매수/매도 우위 판단
     * 호가창을 보고 매수세/매도세 판단
     */
    @JsonIgnore
    public String getMarketPressure() {
        if (bestBid == null || bestAsk == null) {
            return "NEUTRAL";
        }
        
        // 매수호가가 매도호가에 가까울수록 매수세
        long midPrice = (bestBid + bestAsk) / 2;
        
        if (price >= midPrice) {
            return "BUY_PRESSURE";  // 매수세
        } else {
            return "SELL_PRESSURE"; // 매도세
        }
    }
}
