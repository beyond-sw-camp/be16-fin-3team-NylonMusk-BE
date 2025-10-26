package com.beyond.MKX.domain.market.dto;

import com.beyond.MKX.domain.orderbook.entity.OrderBook;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 통합 마켓 데이터 DTO
 * 
 * 호가창에서 필요한 모든 데이터를 하나의 API 응답으로 제공
 * - 현재가 정보
 * - 52주 정보
 * - 거래량 정보
 * - 체결강도
 * - 호가 정보
 * - 사용자 주문 (선택사항)
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataDTO {
    
    // ========== 현재가 정보 ==========
    private long currentPrice;          // 현재가 (0 = 데이터 없음)
    private long prevClose;             // 전일 종가 (0 = 데이터 없음)
    private long open;                  // 시가 (0 = 데이터 없음)
    private long high;                  // 고가 (0 = 데이터 없음)
    private long low;                   // 저가 (0 = 데이터 없음)
    private long change;                // 전일대비 등락액
    private BigDecimal changeRate;      // 전일대비 등락률 (%)
    
    // ========== 거래량 정보 ==========
    private BigDecimal volume;          // 당일 누적 거래량
    private BigDecimal volumeChange;    // 거래량 변화율 (%) (0 = 데이터 없음)
    private BigDecimal prevVolume;      // 전일 거래량
    
    // ========== 52주 정보 ==========
    private long week52High;            // 52주 최고가 (0 = 데이터 없음)
    private long week52Low;             // 52주 최저가 (0 = 데이터 없음)
    
    // ========== 체결강도 ==========
    private BigDecimal executionStrength; // 체결강도 (0 = 데이터 없음)
    
    // ========== 호가 정보 ==========
    private List<OrderBook.OrderBookEntry> bids; // 매수 호가
    private List<OrderBook.OrderBookEntry> asks; // 매도 호가
    
    // ========== 사용자 주문 (Optional) ==========
    private List<Long> userOrders;      // 사용자 주문 가격 목록
    
    // ========== 타임스탬프 ==========
    private Instant timestamp;          // 데이터 생성 시각
}
