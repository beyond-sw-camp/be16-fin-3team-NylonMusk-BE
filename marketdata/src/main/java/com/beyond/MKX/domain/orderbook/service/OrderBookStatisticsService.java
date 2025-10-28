package com.beyond.MKX.domain.orderbook.service;

import com.beyond.MKX.domain.orderbook.dto.enhanced.OrderBookStatistics;
import com.beyond.MKX.domain.orderbook.entity.OrderBook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

/**
 * 호가창 통계 계산 서비스
 * 
 * 호가 데이터를 기반으로 각종 통계 지표 계산
 * - 중간호가, 스프레드
 * - 체결강도 (최근 5분간 매수/매도 체결량 기반)
 * - 총 매수/매도 잔량
 * - 호가 깊이
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBookStatisticsService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Redis key prefix for execution volume tracking
    private static final String EXECUTION_VOLUME_KEY_PREFIX = "execution:volume:";
    
    // 체결강도 계산 윈도우 (5분)
    private static final long EXECUTION_STRENGTH_WINDOW_MINUTES = 5;
    
    /**
     * 호가창 통계 계산
     * 
     * @param orderBook 호가 데이터
     * @return OrderBookStatistics
     */
    public OrderBookStatistics calculateStatistics(OrderBook orderBook) {
        try {
            // 1. 최우선 호가 정보
            OrderBook.OrderBookEntry bestBid = orderBook.getBestBid();
            OrderBook.OrderBookEntry bestAsk = orderBook.getBestAsk();
            
            // 2. 중간호가 및 스프레드
            Long midPrice = calculateMidPrice(bestBid, bestAsk);
            Long spreadAmount = calculateSpreadAmount(bestBid, bestAsk);
            BigDecimal spreadPercent = calculateSpreadPercent(spreadAmount, midPrice);
            
            // 3. 체결강도 (Redis에서 최근 체결량 조회)
            String ticker = orderBook.getTicker();
            BigDecimal recentBuyVolume = getRecentVolume(ticker, "buyVolume");
            BigDecimal recentSellVolume = getRecentVolume(ticker, "sellVolume");
            BigDecimal executionStrength = calculateExecutionStrength(recentBuyVolume, recentSellVolume);
            
            // 4. 총 매수/매도 잔량
            BigDecimal totalBidVolume = calculateTotalVolume(orderBook.getBids());
            BigDecimal totalAskVolume = calculateTotalVolume(orderBook.getAsks());
            
            // 5. 매수/매도 우위 비율
            BigDecimal bidRatio = calculateRatio(totalBidVolume, totalBidVolume, totalAskVolume);
            BigDecimal askRatio = calculateRatio(totalAskVolume, totalBidVolume, totalAskVolume);
            
            // 6. 호가 깊이
            int bidDepth = orderBook.getBids().size();
            int askDepth = orderBook.getAsks().size();
            int totalDepth = bidDepth + askDepth;
            
            OrderBookStatistics statistics = OrderBookStatistics.builder()
                    // 가격 관련 통계
                    .midPrice(midPrice)
                    .spreadAmount(spreadAmount)
                    .spreadPercent(spreadPercent)
                    
                    // 체결강도
                    .executionStrength(executionStrength)
                    .recentBuyVolume(recentBuyVolume)
                    .recentSellVolume(recentSellVolume)
                    
                    // 호가 잔량 통계
                    .totalBidVolume(totalBidVolume)
                    .totalAskVolume(totalAskVolume)
                    .bidRatio(bidRatio)
                    .askRatio(askRatio)
                    
                    // 호가 깊이
                    .bidDepth(bidDepth)
                    .askDepth(askDepth)
                    .totalDepth(totalDepth)
                    
                    // 최우선 호가 정보
                    .bestBidPrice(bestBid != null ? bestBid.getPrice() : null)
                    .bestBidQuantity(bestBid != null ? bestBid.getQuantity() : BigDecimal.ZERO)
                    .bestAskPrice(bestAsk != null ? bestAsk.getPrice() : null)
                    .bestAskQuantity(bestAsk != null ? bestAsk.getQuantity() : BigDecimal.ZERO)
                    
                    // 타임스탬프
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            log.debug("[ORDERBOOK-STATS] Calculated: ticker={}, midPrice={}, spread={}, strength={}, bidVol={}, askVol={}", 
                    ticker, midPrice, spreadAmount, executionStrength, totalBidVolume, totalAskVolume);
            
            return statistics;
            
        } catch (Exception e) {
            log.error("[ORDERBOOK-STATS] Failed to calculate statistics: ticker={}", 
                    orderBook.getTicker(), e);
            return createEmptyStatistics();
        }
    }
    
    /**
     * 중간호가 계산
     * (최우선 매수호가 + 최우선 매도호가) / 2
     */
    private Long calculateMidPrice(OrderBook.OrderBookEntry bestBid, OrderBook.OrderBookEntry bestAsk) {
        if (bestBid != null && bestAsk != null) {
            return (bestBid.getPrice() + bestAsk.getPrice()) / 2;
        }
        return null;
    }
    
    /**
     * 스프레드 계산 (절대값)
     * 최우선 매도호가 - 최우선 매수호가
     */
    private Long calculateSpreadAmount(OrderBook.OrderBookEntry bestBid, OrderBook.OrderBookEntry bestAsk) {
        if (bestBid != null && bestAsk != null) {
            return bestAsk.getPrice() - bestBid.getPrice();
        }
        return null;
    }
    
    /**
     * 스프레드 비율 계산 (%)
     * spreadAmount / midPrice * 100
     */
    private BigDecimal calculateSpreadPercent(Long spreadAmount, Long midPrice) {
        if (spreadAmount != null && midPrice != null && midPrice > 0) {
            return BigDecimal.valueOf(spreadAmount)
                    .divide(BigDecimal.valueOf(midPrice), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Redis에서 최근 체결량 조회
     * 
     * @param ticker 종목코드
     * @param field "buyVolume" 또는 "sellVolume"
     * @return 체결량
     */
    private BigDecimal getRecentVolume(String ticker, String field) {
        try {
            String key = EXECUTION_VOLUME_KEY_PREFIX + ticker;
            Object value = redisTemplate.opsForHash().get(key, field);
            
            if (value != null) {
                if (value instanceof Number) {
                    return BigDecimal.valueOf(((Number) value).doubleValue());
                } else if (value instanceof String) {
                    return new BigDecimal((String) value);
                }
            }
        } catch (Exception e) {
            log.warn("[ORDERBOOK-STATS] Failed to get recent volume: ticker={}, field={}", ticker, field, e);
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * 체결강도 계산
     * (매수체결량 / 매도체결량) * 100
     * 
     * 해석:
     * - 100 이상: 매수세 우위
     * - 100 이하: 매도세 우위
     */
    private BigDecimal calculateExecutionStrength(BigDecimal buyVolume, BigDecimal sellVolume) {
        if (sellVolume.compareTo(BigDecimal.ZERO) > 0) {
            return buyVolume.divide(sellVolume, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        
        // 매도체결량이 0인 경우
        if (buyVolume.compareTo(BigDecimal.ZERO) > 0) {
            return BigDecimal.valueOf(200); // 매수세 극대
        }
        
        return BigDecimal.valueOf(100); // 기본값 (균형)
    }
    
    /**
     * 총 호가 수량 계산
     */
    private BigDecimal calculateTotalVolume(java.util.List<OrderBook.OrderBookEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return entries.stream()
                .map(OrderBook.OrderBookEntry::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 비율 계산
     * 
     * @param volume 대상 수량
     * @param totalBidVolume 총 매수 잔량
     * @param totalAskVolume 총 매도 잔량
     * @return 비율 (%)
     */
    private BigDecimal calculateRatio(BigDecimal volume, BigDecimal totalBidVolume, BigDecimal totalAskVolume) {
        BigDecimal total = totalBidVolume.add(totalAskVolume);
        
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            return volume.divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        
        return BigDecimal.valueOf(50); // 기본값 (균형)
    }
    
    /**
     * 체결량 누적 (Redis)
     * ExecutionKafkaConsumer에서 호출
     * 
     * @param ticker 종목코드
     * @param side "BUY" 또는 "SELL"
     * @param quantity 체결량
     */
    public void updateExecutionVolume(String ticker, String side, BigDecimal quantity) {
        try {
            String key = EXECUTION_VOLUME_KEY_PREFIX + ticker;
            String field = "BUY".equalsIgnoreCase(side) ? "buyVolume" : "sellVolume";
            
            // 체결량 누적
            redisTemplate.opsForHash().increment(key, field, quantity.doubleValue());
            
            // TTL 설정 (5분)
            redisTemplate.expire(key, EXECUTION_STRENGTH_WINDOW_MINUTES, TimeUnit.MINUTES);
            
            log.debug("[ORDERBOOK-STATS] Updated execution volume: ticker={}, side={}, qty={}", 
                    ticker, side, quantity);
            
        } catch (Exception e) {
            log.error("[ORDERBOOK-STATS] Failed to update execution volume: ticker={}, side={}", 
                    ticker, side, e);
        }
    }
    
    /**
     * 체결량 초기화 (테스트용)
     */
    public void resetExecutionVolume(String ticker) {
        try {
            String key = EXECUTION_VOLUME_KEY_PREFIX + ticker;
            redisTemplate.delete(key);
            log.info("[ORDERBOOK-STATS] Reset execution volume: ticker={}", ticker);
        } catch (Exception e) {
            log.error("[ORDERBOOK-STATS] Failed to reset execution volume: ticker={}", ticker, e);
        }
    }
    
    /**
     * 빈 통계 객체 생성
     */
    private OrderBookStatistics createEmptyStatistics() {
        return OrderBookStatistics.builder()
                .midPrice(null)
                .spreadAmount(null)
                .spreadPercent(BigDecimal.ZERO)
                .executionStrength(BigDecimal.valueOf(100))
                .recentBuyVolume(BigDecimal.ZERO)
                .recentSellVolume(BigDecimal.ZERO)
                .totalBidVolume(BigDecimal.ZERO)
                .totalAskVolume(BigDecimal.ZERO)
                .bidRatio(BigDecimal.valueOf(50))
                .askRatio(BigDecimal.valueOf(50))
                .bidDepth(0)
                .askDepth(0)
                .totalDepth(0)
                .bestBidPrice(null)
                .bestBidQuantity(BigDecimal.ZERO)
                .bestAskPrice(null)
                .bestAskQuantity(BigDecimal.ZERO)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
