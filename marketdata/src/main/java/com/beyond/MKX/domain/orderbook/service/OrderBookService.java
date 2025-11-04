package com.beyond.MKX.domain.orderbook.service;

import com.beyond.MKX.domain.execution.dto.ExecutionEventDTO;
import com.beyond.MKX.domain.orderbook.entity.OrderBook;
import com.beyond.MKX.domain.orderbook.event.OrderBookEventPublisher;
import com.beyond.MKX.domain.orderbook.repository.MatchingEngineOrderBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 호가 관리 서비스
 * 
 * ✅ 변경: matching-engine이 관리하는 Redis에서 직접 orderbook 조회
 * - 더 이상 자체 Redis에 orderbook을 저장/관리하지 않음
 * - matching-engine Redis Cluster에서 실시간 조회만 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBookService {

    private final MatchingEngineOrderBookRepository matchingEngineOrderBookRepository;
    private final OrderBookEventPublisher eventPublisher;

    /**
     * 체결 이벤트 발생 시 WebSocket 업데이트만 발행
     * 
     * ✅ 변경사항:
     * - 기존: orderbook을 수정하여 자체 Redis에 저장
     * - 변경: matching-engine이 이미 orderbook을 관리하므로, 
     *         단순히 WebSocket 업데이트 이벤트만 발행
     */
    public void updateOrderBookAfterExecution(ExecutionEventDTO execution) {
        try {
            String ticker = execution.getTicker();
            log.debug("[ORDERBOOK/EXEC] Execution event received: ticker={}, side={}, price={}, qty={}", 
                    ticker, execution.getSide(), execution.getPrice(), execution.getQuantity());
            
            // matching-engine이 이미 orderbook을 관리하므로, 업데이트 이벤트만 발행
            eventPublisher.publishEnhancedUpdate(ticker);
            
        } catch (Exception e) {
            log.error("[ORDERBOOK/EXEC] ❌ Failed to publish update event: ticker={}", 
                    execution.getTicker(), e);
        }
    }

    /**
     * 호가 업데이트 이벤트 트리거
     * 
     * 주문 상태 변경(신규 주문 추가, 체결, 취소 등) 시 호가 업데이트 이벤트를 발행
     * matching-engine이 orderbook을 관리하므로, 단순히 업데이트 이벤트만 발행
     * 
     * @param ticker 종목코드
     */
    public void triggerOrderBookUpdate(String ticker) {
        try {
            log.debug("[ORDERBOOK] Triggering update event: ticker={}", ticker);
            eventPublisher.publishEnhancedUpdate(ticker);
            log.info("[ORDERBOOK] ✅ Enhanced update event published: ticker={}", ticker);
        } catch (Exception e) {
            log.error("[ORDERBOOK] ❌ Failed to trigger update event: ticker={}", ticker, e);
        }
    }

    /**
     * 호가 조회 (matching-engine Redis에서 직접 조회)
     * 
     * ✅ 변경사항:
     * - 기존: 자체 Redis에서 orderbook:{ticker} 조회
     * - 변경: matching-engine Redis Cluster에서 orderbook:{ticker}:bids/asks 조회
     */
    public OrderBook getOrderBook(String ticker) {
        try {
            return matchingEngineOrderBookRepository.getOrderBook(ticker);
        } catch (Exception e) {
            log.error("[ORDERBOOK] ❌ Failed to get orderbook: ticker={}", ticker, e);
            return OrderBook.createEmpty(ticker);
        }
    }

    // ========== Deprecated: 더 이상 사용하지 않는 메서드들 ==========
    // matching-engine이 orderbook을 직접 관리하므로 자체 저장/관리는 불필요
    // 하지만 하위 호환성을 위해 유지 (빈 구현 또는 이벤트 발행만 수행)

    /**
     * @deprecated matching-engine이 orderbook을 직접 관리하므로 더 이상 사용하지 않음
     * 테스트용 API 호환성을 위해 유지
     */
    @Deprecated
    public void initializeOrderBook(String ticker) {
        log.debug("[ORDERBOOK] initializeOrderBook called but deprecated - matching-engine manages orderbook directly");
        eventPublisher.publishEnhancedUpdate(ticker);
    }

    /**
     * @deprecated matching-engine이 orderbook을 직접 관리하므로 더 이상 사용하지 않음
     * 테스트용 API 호환성을 위해 유지
     */
    @Deprecated
    public void deleteOrderBook(String ticker) {
        log.debug("[ORDERBOOK] deleteOrderBook called but deprecated - matching-engine manages orderbook directly");
        eventPublisher.publishEnhancedUpdate(ticker);
    }

    // ========== 주문 상태 이벤트 기반 호가 관리 ==========

    /**
     * @deprecated matching-engine이 orderbook을 직접 관리하므로 더 이상 사용하지 않음
     */
    @Deprecated
    public void addBid(String ticker, long price, BigDecimal quantity) {
        log.debug("[ORDERBOOK] addBid called but deprecated - matching-engine manages orderbook directly");
        eventPublisher.publishEnhancedUpdate(ticker);
    }

    /**
     * @deprecated matching-engine이 orderbook을 직접 관리하므로 더 이상 사용하지 않음
     */
    @Deprecated
    public void addAsk(String ticker, long price, BigDecimal quantity) {
        log.debug("[ORDERBOOK] addAsk called but deprecated - matching-engine manages orderbook directly");
        eventPublisher.publishEnhancedUpdate(ticker);
    }

    /**
     * @deprecated matching-engine이 orderbook을 직접 관리하므로 더 이상 사용하지 않음
     */
    @Deprecated
    public void updateBidQuantity(String ticker, long price, BigDecimal remainingQuantity) {
        log.debug("[ORDERBOOK] updateBidQuantity called but deprecated - matching-engine manages orderbook directly");
        eventPublisher.publishEnhancedUpdate(ticker);
    }

    /**
     * @deprecated matching-engine이 orderbook을 직접 관리하므로 더 이상 사용하지 않음
     */
    @Deprecated
    public void updateAskQuantity(String ticker, long price, BigDecimal remainingQuantity) {
        log.debug("[ORDERBOOK] updateAskQuantity called but deprecated - matching-engine manages orderbook directly");
        eventPublisher.publishEnhancedUpdate(ticker);
    }

    /**
     * @deprecated matching-engine이 orderbook을 직접 관리하므로 더 이상 사용하지 않음
     */
    @Deprecated
    public void removeBidCompletely(String ticker, long price) {
        log.debug("[ORDERBOOK] removeBidCompletely called but deprecated - matching-engine manages orderbook directly");
        eventPublisher.publishEnhancedUpdate(ticker);
    }

    /**
     * @deprecated matching-engine이 orderbook을 직접 관리하므로 더 이상 사용하지 않음
     */
    @Deprecated
    public void removeAskCompletely(String ticker, long price) {
        log.debug("[ORDERBOOK] removeAskCompletely called but deprecated - matching-engine manages orderbook directly");
        eventPublisher.publishEnhancedUpdate(ticker);
    }

    // ========== OrderStatusEventDTO 기반 호가 관리 (Kafka Consumer용) ==========

    /**
     * @deprecated matching-engine이 orderbook을 직접 관리하므로 더 이상 사용하지 않음
     */
    @Deprecated
    public void addOrderToBook(com.beyond.MKX.domain.orderbook.dto.OrderStatusEventDTO orderStatus) {
        log.debug("[ORDERBOOK] addOrderToBook called but deprecated - matching-engine manages orderbook directly");
        eventPublisher.publishEnhancedUpdate(orderStatus.getTicker());
    }

    /**
     * @deprecated matching-engine이 orderbook을 직접 관리하므로 더 이상 사용하지 않음
     */
    @Deprecated
    public void removeOrderFromBook(com.beyond.MKX.domain.orderbook.dto.OrderStatusEventDTO orderStatus) {
        log.debug("[ORDERBOOK] removeOrderFromBook called but deprecated - matching-engine manages orderbook directly");
        eventPublisher.publishEnhancedUpdate(orderStatus.getTicker());
    }

    /**
     * @deprecated matching-engine이 orderbook을 직접 관리하므로 더 이상 사용하지 않음
     */
    @Deprecated
    public void updateOrderQuantity(com.beyond.MKX.domain.orderbook.dto.OrderStatusEventDTO orderStatus) {
        log.debug("[ORDERBOOK] updateOrderQuantity called but deprecated - matching-engine manages orderbook directly");
        eventPublisher.publishEnhancedUpdate(orderStatus.getTicker());
    }
    
    // ✅ 제거: WebSocket 의존성 제거로 인해 삭제
    // WebSocket 세션 수는 WebSocketHandler에서 직접 조회해야 함
}
