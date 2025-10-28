package com.beyond.MKX.domain.orderbook.dto.enhanced;

import com.beyond.MKX.domain.orderbook.entity.OrderBook;
import lombok.*;

import java.util.List;

/**
 * 고도화된 호가창 데이터 DTO
 * 
 * 기본 호가 정보 + 시장 요약 + 통계 정보를 통합한 DTO
 * WebSocket을 통해 클라이언트에게 전송
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EnhancedOrderBookDTO {
    
    /**
     * 종목 코드
     */
    private String ticker;
    
    /**
     * 시장 요약 정보
     * - 52주 최고/최저가
     * - 상한가/하한가
     * - 당일 가격 정보
     * - 거래량 및 등락 정보
     */
    private MarketSummary marketSummary;
    
    /**
     * 매수 호가 리스트 (높은 가격 순)
     */
    private List<OrderBook.OrderBookEntry> bids;
    
    /**
     * 매도 호가 리스트 (낮은 가격 순)
     */
    private List<OrderBook.OrderBookEntry> asks;
    
    /**
     * 호가창 통계 정보
     * - 중간호가, 스프레드
     * - 체결강도
     * - 총 매수/매도 잔량
     */
    private OrderBookStatistics statistics;
    
    /**
     * 마지막 업데이트 시각 (Unix timestamp, milliseconds)
     */
    private Long timestamp;
    
    /**
     * 데이터 버전 (선택적)
     * 클라이언트 사이드 캐싱 및 동기화에 사용
     */
    private String version;
}
