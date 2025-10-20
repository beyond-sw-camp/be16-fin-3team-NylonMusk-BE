package com.beyond.MKX.domain.orderbook.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 호가 엔티티
 * 
 * 특정 종목의 매수/매도 호가 정보를 담는 모델
 * Redis에 저장되어 실시간으로 업데이트
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderBook {
    
    private String ticker;                    // 종목 코드
    private List<OrderBookEntry> bids;        // 매수 호가 (높은 가격 순)
    private List<OrderBookEntry> asks;        // 매도 호가 (낮은 가격 순)
    private long timestamp;                   // 마지막 업데이트 시각
    
    /**
     * 호가 항목 (가격, 수량)
     */
    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class OrderBookEntry {
        private long price;                   // 호가 가격
        private BigDecimal quantity;          // 호가 수량
        
        public OrderBookEntry(long price) {
            this.price = price;
            this.quantity = BigDecimal.ZERO;
        }
    }
    
    /**
     * 초기화
     */
    public static OrderBook createEmpty(String ticker) {
        return OrderBook.builder()
                .ticker(ticker)
                .bids(new ArrayList<>())
                .asks(new ArrayList<>())
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 매수 호가 추가/업데이트
     */
    public void addBid(long price, BigDecimal quantity) {
        addOrUpdateEntry(bids, price, quantity, true);
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 매도 호가 추가/업데이트
     */
    public void addAsk(long price, BigDecimal quantity) {
        addOrUpdateEntry(asks, price, quantity, false);
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 호가 제거 (체결 완료된 경우)
     * @return 제거 성공 여부
     */
    public boolean removeBid(long price, BigDecimal quantity) {
        boolean removed = removeEntry(bids, price, quantity);
        if (removed) {
            this.timestamp = System.currentTimeMillis();
        }
        return removed;
    }
    
    /**
     * 호가 제거 (체결 완료된 경우)
     * @return 제거 성공 여부
     */
    public boolean removeAsk(long price, BigDecimal quantity) {
        boolean removed = removeEntry(asks, price, quantity);
        if (removed) {
            this.timestamp = System.currentTimeMillis();
        }
        return removed;
    }
    
    /**
     * 호가 추가/업데이트 내부 메서드
     */
    private void addOrUpdateEntry(List<OrderBookEntry> entries, long price, 
                                  BigDecimal quantity, boolean isBid) {
        // 기존 호가 찾기
        OrderBookEntry existingEntry = entries.stream()
                .filter(e -> e.getPrice() == price)
                .findFirst()
                .orElse(null);
        
        if (existingEntry != null) {
            // 수량 업데이트
            existingEntry.setQuantity(existingEntry.getQuantity().add(quantity));
            
            // 수량이 0 이하면 제거
            if (existingEntry.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                entries.remove(existingEntry);
            }
        } else {
            // 새 호가 추가
            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                entries.add(new OrderBookEntry(price, quantity));
            }
        }
        
        // 정렬 (매수: 높은 가격 순, 매도: 낮은 가격 순)
        entries.sort((a, b) -> isBid ? 
                Long.compare(b.getPrice(), a.getPrice()) : 
                Long.compare(a.getPrice(), b.getPrice()));
        
        // 상위 N개만 유지 (깊이 제한)
        int maxDepth = 20;
        if (entries.size() > maxDepth) {
            entries.subList(maxDepth, entries.size()).clear();
        }
    }
    
    /**
     * 호가 제거 내부 메서드
     * @return 제거 성공 여부
     */
    private boolean removeEntry(List<OrderBookEntry> entries, long price, BigDecimal quantity) {
        OrderBookEntry entry = entries.stream()
                .filter(e -> e.getPrice() == price)
                .findFirst()
                .orElse(null);
        
        if (entry != null) {
            BigDecimal newQuantity = entry.getQuantity().subtract(quantity);
            
            if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                entries.remove(entry);
            } else {
                entry.setQuantity(newQuantity);
            }
            return true;
        }
        
        return false; // 해당 가격의 호가를 찾지 못함
    }
    
    /**
     * 최우선 매수호가 (최고가)
     */
    @JsonIgnore
    public OrderBookEntry getBestBid() {
        return bids.isEmpty() ? null : bids.get(0);
    }
    
    /**
     * 최우선 매도호가 (최저가)
     */
    @JsonIgnore
    public OrderBookEntry getBestAsk() {
        return asks.isEmpty() ? null : asks.get(0);
    }
    
    /**
     * 스프레드 (최우선 매도호가 - 최우선 매수호가)
     */
    @JsonIgnore
    public Long getSpread() {
        OrderBookEntry bestBid = getBestBid();
        OrderBookEntry bestAsk = getBestAsk();
        
        if (bestBid != null && bestAsk != null) {
            return bestAsk.getPrice() - bestBid.getPrice();
        }
        return null;
    }
}
