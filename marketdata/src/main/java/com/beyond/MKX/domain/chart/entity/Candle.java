package com.beyond.MKX.domain.chart.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 캔들 엔티티
 * 
 * OHLCV (Open, High, Low, Close, Volume) 캔들스틱 차트 데이터 모델
 * Redis에 캐싱하고 InfluxDB에서 조회
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Candle {
    
    private String ticker;           // 종목 코드
    private String interval;         // 캔들 간격 (1m, 5m, 15m, 1h, 1d)
    private Instant time;            // 캔들 시작 시각
    
    private long open;               // 시가
    private long high;               // 고가
    private long low;                // 저가
    private long close;              // 종가
    private BigDecimal volume;       // 거래량
    
    /**
     * 새로운 체결 데이터로 캔들 업데이트
     */
    public void update(long price, BigDecimal quantity) {
        // 첫 체결인 경우 시가 설정
        if (this.open == 0) {
            this.open = price;
        }
        
        // 고가 업데이트
        if (price > this.high || this.high == 0) {
            this.high = price;
        }
        
        // 저가 업데이트
        if (price < this.low || this.low == 0) {
            this.low = price;
        }
        
        // 종가는 항상 최신 가격으로 업데이트
        this.close = price;
        
        // 거래량 누적
        if (this.volume == null) {
            this.volume = BigDecimal.ZERO;
        }
        this.volume = this.volume.add(quantity);
    }
}
