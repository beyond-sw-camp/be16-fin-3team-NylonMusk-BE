package com.beyond.MKX.domain.orderbook.service;

import com.beyond.MKX.domain.orderbook.dto.enhanced.EnhancedOrderBookDTO;
import com.beyond.MKX.domain.orderbook.dto.enhanced.MarketSummary;
import com.beyond.MKX.domain.orderbook.dto.enhanced.OrderBookStatistics;
import com.beyond.MKX.domain.orderbook.entity.OrderBook;
import com.beyond.MKX.domain.market.service.MarketSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 고도화된 호가 서비스 확장
 * 
 * OrderBookService의 확장 기능을 제공
 * - 시장 요약 정보 통합
 * - 호가창 통계 계산
 * - 고도화된 호가 데이터 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedOrderBookService {
    
    private final OrderBookService orderBookService;
    private final MarketSummaryService marketSummaryService;
    private final OrderBookStatisticsService orderBookStatisticsService;
    
    /**
     * 고도화된 호가 데이터 조회
     * 
     * 기본 호가 + 시장 요약 + 통계 정보를 통합하여 반환
     * 
     * @param ticker 종목코드
     * @return EnhancedOrderBookDTO
     */
    public EnhancedOrderBookDTO getEnhancedOrderBook(String ticker) {
        try {
            log.debug("[ENHANCED-ORDERBOOK] Fetching enhanced data for ticker: {}", ticker);
            
            // 1. 기본 호가 조회
            OrderBook orderBook = orderBookService.getOrderBook(ticker);
            if (orderBook == null) {
                log.warn("[ENHANCED-ORDERBOOK] No orderbook found for ticker: {}", ticker);
                return createEmptyEnhancedOrderBook(ticker);
            }
            
            // 2. 시장 요약 정보
            MarketSummary marketSummary = marketSummaryService.buildMarketSummary(ticker);
            
            // 3. 호가창 통계
            OrderBookStatistics statistics = orderBookStatisticsService.calculateStatistics(orderBook);
            
            // 4. 통합 DTO 생성
            EnhancedOrderBookDTO enhancedDTO = EnhancedOrderBookDTO.builder()
                    .ticker(ticker)
                    .marketSummary(marketSummary)
                    .bids(orderBook.getBids())
                    .asks(orderBook.getAsks())
                    .statistics(statistics)
                    .timestamp(System.currentTimeMillis())
                    .version("v2.0")
                    .build();
            
            log.info("[ENHANCED-ORDERBOOK] ✅ Built enhanced orderbook: ticker={}, bids={}, asks={}, midPrice={}, strength={}%", 
                    ticker, orderBook.getBids().size(), orderBook.getAsks().size(), 
                    statistics.getMidPrice(), statistics.getExecutionStrength());
            
            return enhancedDTO;
            
        } catch (Exception e) {
            log.error("[ENHANCED-ORDERBOOK] ❌ Failed to build enhanced orderbook for ticker: {}", ticker, e);
            return createEmptyEnhancedOrderBook(ticker);
        }
    }
    
    /**
     * 빈 고도화 호가 DTO 생성 (오류 시)
     */
    private EnhancedOrderBookDTO createEmptyEnhancedOrderBook(String ticker) {
        return EnhancedOrderBookDTO.builder()
                .ticker(ticker)
                .marketSummary(null)
                .bids(Collections.emptyList())
                .asks(Collections.emptyList())
                .statistics(null)
                .timestamp(System.currentTimeMillis())
                .version("v2.0")
                .build();
    }
}
