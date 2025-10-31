package com.beyond.MKX.domain.orderbook.service;

import com.beyond.MKX.domain.execution.dto.ExecutionEventDTO;
import com.beyond.MKX.domain.orderbook.entity.OrderBook;
import com.beyond.MKX.domain.orderbook.event.OrderBookEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * 호가 관리 서비스
 * 
 * Redis에 호가 데이터를 저장하고 실시간으로 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBookService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderBookEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    
    // ✅ 제거: 고도화 기능 의존성 제거 (이벤트 기반으로 전환)
    // private final com.beyond.MKX.domain.market.service.MarketSummaryService marketSummaryService;
    // private final OrderBookStatisticsService orderBookStatisticsService;

    // Redis key prefix
    private static final String ORDERBOOK_KEY_PREFIX = "orderbook:";
    
    // 호가 데이터 TTL (1시간)
    private static final long ORDERBOOK_TTL_MINUTES = 60;

    /**
     * 체결 이벤트 발생 시 호가 업데이트
     * 
     * ✅ 수정된 로직:
     * - 체결은 "시장가 주문(marketOrder)"과 "지정가 주문(counterOrder)"이 만남
     * - 시장가 BUY는 호가의 매도(Ask)와 체결 → 매도 호가 차감
     * - 시장가 SELL은 호가의 매수(Bid)와 체결 → 매수 호가 차감
     * - counterOrder는 이미 호가에서 OrderStatusEvent로 제거됨
     */
    public void updateOrderBookAfterExecution(ExecutionEventDTO execution) {
        try {
            String ticker = execution.getTicker();
            OrderBook orderBook = getOrderBook(ticker);
            
            if (orderBook == null) {
                log.warn("[ORDERBOOK/EXEC] OrderBook not found for ticker: {}", ticker);
                orderBook = OrderBook.createEmpty(ticker);
            }
            
            log.info("[ORDERBOOK/EXEC] Processing execution: ticker={}, side={}, price={}, qty={}", 
                    ticker, execution.getSide(), execution.getPrice(), execution.getQuantity());
            
            // ✅ 체결 로직 수정
            // marketOrder.side = BUY  → 매도호가(Ask)와 체결 → Ask 차감
            // marketOrder.side = SELL → 매수호가(Bid)와 체결 → Bid 차감
            if ("BUY".equals(execution.getSide())) {
                // 시장가 매수 → 매도 호가에서 차감
                boolean removed = orderBook.removeAsk(execution.getPrice(), execution.getQuantity());
                log.debug("[ORDERBOOK/EXEC] Removed from ASK: ticker={}, price={}, qty={}, success={}", 
                        ticker, execution.getPrice(), execution.getQuantity(), removed);
            } else if ("SELL".equals(execution.getSide())) {
                // 시장가 매도 → 매수 호가에서 차감
                boolean removed = orderBook.removeBid(execution.getPrice(), execution.getQuantity());
                log.debug("[ORDERBOOK/EXEC] Removed from BID: ticker={}, price={}, qty={}, success={}", 
                        ticker, execution.getPrice(), execution.getQuantity(), removed);
            }
            
            // Redis에 저장
            saveOrderBook(orderBook);
            
            // ✅ 변경: 이벤트 발행으로 WebSocket 전송
            eventPublisher.publishEnhancedUpdate(ticker);
            
            log.info("[ORDERBOOK/EXEC] ✅ Updated orderbook: ticker={}, bids={}, asks={}", 
                    ticker, orderBook.getBids().size(), orderBook.getAsks().size());
            
        } catch (Exception e) {
            log.error("[ORDERBOOK/EXEC] ❌ Failed to update orderbook", e);
        }
    }

    /**
     * 호가 조회 (Redis에서) - 안전한 역직렬화
     */
    public OrderBook getOrderBook(String ticker) {
        try {
            String redisKey = buildRedisKey(ticker);
            Object data = redisTemplate.opsForValue().get(redisKey);
            
            if (data == null) {
                log.debug("No orderbook found for ticker: {}, creating empty orderbook", ticker);
                return OrderBook.createEmpty(ticker);
            }
            
            // 이미 OrderBook 타입인 경우
            if (data instanceof OrderBook) {
                return (OrderBook) data;
            }
            
            // LinkedHashMap 등 다른 타입인 경우 ObjectMapper로 변환
            return objectMapper.convertValue(data, OrderBook.class);
            
        } catch (Exception e) {
            log.error("Failed to get orderbook from Redis: ticker={}", ticker, e);
            return OrderBook.createEmpty(ticker);
        }
    }

    /**
     * 호가 저장 (Redis에)
     */
    public void saveOrderBook(OrderBook orderBook) {
        try {
            String redisKey = buildRedisKey(orderBook.getTicker());
            orderBook.setTimestamp(System.currentTimeMillis()); // ✅ timestamp 업데이트
            redisTemplate.opsForValue().set(redisKey, orderBook, 
                    ORDERBOOK_TTL_MINUTES, TimeUnit.MINUTES);
            
            log.debug("Saved orderbook to Redis: ticker={}, bids={}, asks={}", 
                    orderBook.getTicker(), orderBook.getBids().size(), orderBook.getAsks().size());
            
        } catch (Exception e) {
            log.error("Failed to save orderbook to Redis: ticker={}", 
                    orderBook.getTicker(), e);
        }
    }

    /**
     * 호가 초기화 (테스트용)
     */
    public void initializeOrderBook(String ticker) {
        OrderBook orderBook = OrderBook.createEmpty(ticker);
        saveOrderBook(orderBook);
        log.info("Initialized orderbook for ticker: {}", ticker);
    }

    /**
     * 호가 삭제
     */
    public void deleteOrderBook(String ticker) {
        try {
            String redisKey = buildRedisKey(ticker);
            redisTemplate.delete(redisKey);
            log.info("Deleted orderbook for ticker: {}", ticker);
        } catch (Exception e) {
            log.error("Failed to delete orderbook: ticker={}", ticker, e);
        }
    }

    /**
     * Redis key 생성
     */
    private String buildRedisKey(String ticker) {
        return ORDERBOOK_KEY_PREFIX + ticker;
    }

    // ========== 주문 상태 이벤트 기반 호가 관리 ==========

    /**
     * 매수 호가 추가
     */
    public void addBid(String ticker, long price, BigDecimal quantity) {
        OrderBook orderBook = getOrderBook(ticker);
        orderBook.addBid(price, quantity);
        saveOrderBook(orderBook);
        eventPublisher.publishEnhancedUpdate(ticker);
        log.info("[ORDERBOOK/ADD] Added BID: ticker={}, price={}, quantity={}", ticker, price, quantity);
    }

    /**
     * 매도 호가 추가
     */
    public void addAsk(String ticker, long price, BigDecimal quantity) {
        OrderBook orderBook = getOrderBook(ticker);
        orderBook.addAsk(price, quantity);
        saveOrderBook(orderBook);
        eventPublisher.publishEnhancedUpdate(ticker);
        log.info("[ORDERBOOK/ADD] Added ASK: ticker={}, price={}, quantity={}", ticker, price, quantity);
    }

    /**
     * 매수 호가 수량 업데이트 (부분 체결 시)
     */
    public void updateBidQuantity(String ticker, long price, BigDecimal remainingQuantity) {
        OrderBook orderBook = getOrderBook(ticker);
        
        // 기존 호가 찾기
        OrderBook.OrderBookEntry existingEntry = orderBook.getBids().stream()
                .filter(e -> e.getPrice() == price)
                .findFirst()
                .orElse(null);
        
        if (existingEntry != null) {
            // 기존 호가를 제거하고 새 수량으로 교체 (SET 방식)
            orderBook.removeBid(price, existingEntry.getQuantity());
            
            if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
                orderBook.addBid(price, remainingQuantity);
            }
            
            saveOrderBook(orderBook);
            eventPublisher.publishEnhancedUpdate(ticker);
            log.info("[ORDERBOOK/UPDATE] Updated BID: ticker={}, price={}, remaining={}", 
                    ticker, price, remainingQuantity);
        } else {
            log.warn("[ORDERBOOK/UPDATE] No existing BID found: ticker={}, price={}", ticker, price);
        }
    }

    /**
     * 매도 호가 수량 업데이트 (부분 체결 시)
     */
    public void updateAskQuantity(String ticker, long price, BigDecimal remainingQuantity) {
        OrderBook orderBook = getOrderBook(ticker);
        
        // 기존 호가 찾기
        OrderBook.OrderBookEntry existingEntry = orderBook.getAsks().stream()
                .filter(e -> e.getPrice() == price)
                .findFirst()
                .orElse(null);
        
        if (existingEntry != null) {
            // 기존 호가를 제거하고 새 수량으로 교체 (SET 방식)
            orderBook.removeAsk(price, existingEntry.getQuantity());
            
            if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
                orderBook.addAsk(price, remainingQuantity);
            }
            
            saveOrderBook(orderBook);
            eventPublisher.publishEnhancedUpdate(ticker);
            log.info("[ORDERBOOK/UPDATE] Updated ASK: ticker={}, price={}, remaining={}", 
                    ticker, price, remainingQuantity);
        } else {
            log.warn("[ORDERBOOK/UPDATE] No existing ASK found: ticker={}, price={}", ticker, price);
        }
    }

    /**
     * 매수 호가 완전 제거 (완전 체결 또는 취소 시)
     */
    public void removeBidCompletely(String ticker, long price) {
        OrderBook orderBook = getOrderBook(ticker);
        
        BigDecimal quantity = orderBook.getBids().stream()
                .filter(e -> e.getPrice() == price)
                .findFirst()
                .map(OrderBook.OrderBookEntry::getQuantity)
                .orElse(BigDecimal.ZERO);
        
        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            orderBook.removeBid(price, quantity);
            saveOrderBook(orderBook);
            eventPublisher.publishEnhancedUpdate(ticker);
            log.info("[ORDERBOOK/REMOVE] Removed BID: ticker={}, price={}", ticker, price);
        }
    }

    /**
     * 매도 호가 완전 제거 (완전 체결 또는 취소 시)
     */
    public void removeAskCompletely(String ticker, long price) {
        OrderBook orderBook = getOrderBook(ticker);
        
        BigDecimal quantity = orderBook.getAsks().stream()
                .filter(e -> e.getPrice() == price)
                .findFirst()
                .map(OrderBook.OrderBookEntry::getQuantity)
                .orElse(BigDecimal.ZERO);
        
        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            orderBook.removeAsk(price, quantity);
            saveOrderBook(orderBook);
            eventPublisher.publishEnhancedUpdate(ticker);
            log.info("[ORDERBOOK/REMOVE] Removed ASK: ticker={}, price={}", ticker, price);
        }
    }

    // ========== OrderStatusEventDTO 기반 호가 관리 (Kafka Consumer용) ==========

    /**
     * 주문 상태 이벤트 기반 호가 추가
     * NEW_ACCEPTED 또는 WAITING 상태일 때 호출
     * 
     * ✅ v2.0 NULL 방어 로직 강화:
     * - quantity(remaining)가 null이면 에러 로그 + 처리 중단
     * - 필수 필드 검증 강화
     */
    public void addOrderToBook(com.beyond.MKX.domain.orderbook.dto.OrderStatusEventDTO orderStatus) {
        try {
            String orderId = orderStatus.getOrderId();
            String ticker = orderStatus.getTicker();
            String side = orderStatus.getSide();
            long price = orderStatus.getPrice();
            BigDecimal quantity = orderStatus.getRemaining();

            // ✅ NULL 방어 로직 1: 필수 필드 검증
            if (ticker == null || ticker.isBlank()) {
                log.error("[ORDER-STATUS/ADD] ❌ CRITICAL: Ticker is null/empty! orderId={}", orderId);
                return;
            }
            
            if (side == null || side.isBlank()) {
                log.error("[ORDER-STATUS/ADD] ❌ CRITICAL: Side is null/empty! orderId={}, ticker={}", 
                         orderId, ticker);
                return;
            }

            // ✅ NULL 방어 로직 2: quantity(remaining) 검증
            if (quantity == null) {
                log.error("[ORDER-STATUS/ADD] ❌ CRITICAL: Quantity(remaining) is NULL! " +
                         "orderId={}, ticker={}, side={}, price={}. " +
                         "Cannot add order to book without quantity.",
                         orderId, ticker, side, price);
                
                // 모니터링을 위한 전체 데이터 출력
                log.error("[ORDER-STATUS/ADD] ❌ Full orderStatus data: {}", orderStatus);
                return;
            }
            
            // ✅ NULL 방어 로직 3: quantity 범위 검증
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("[ORDER-STATUS/ADD] ❌ CRITICAL: Quantity is zero or negative! " +
                         "orderId={}, quantity={}", orderId, quantity);
                return;
            }

            // 정상 처리
            if ("BUY".equalsIgnoreCase(side)) {
                addBid(ticker, price, quantity);
                log.info("[ORDER-STATUS/ADD] ✅ Added BUY: orderId={}, ticker={}, price={}, quantity={}",
                        orderId, ticker, price, quantity);
            } else if ("SELL".equalsIgnoreCase(side)) {
                addAsk(ticker, price, quantity);
                log.info("[ORDER-STATUS/ADD] ✅ Added SELL: orderId={}, ticker={}, price={}, quantity={}",
                        orderId, ticker, price, quantity);
            } else {
                log.error("[ORDER-STATUS/ADD] ❌ Unknown order side: orderId={}, side={}", 
                        orderId, side);
            }

        } catch (Exception e) {
            log.error("[ORDER-STATUS/ADD] ❌ Exception occurred while adding order: " +
                     "orderId={}, error={}", 
                     orderStatus.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * 주문 상태 이벤트 기반 호가 제거
     * CANCEL_OK 또는 MARKET_FILLED 상태일 때 호출
     * 
     * ✅ v2.0 로그 개선:
     * - 필수 필드 검증
     * - 상세 에러 로깅
     */
    public void removeOrderFromBook(com.beyond.MKX.domain.orderbook.dto.OrderStatusEventDTO orderStatus) {
        try {
            String orderId = orderStatus.getOrderId();
            String ticker = orderStatus.getTicker();
            String side = orderStatus.getSide();
            long price = orderStatus.getPrice();

            // ✅ 필수 필드 검증
            if (ticker == null || ticker.isBlank()) {
                log.error("[ORDER-STATUS/REMOVE] ❌ Ticker is null/empty! orderId={}", orderId);
                return;
            }
            
            if (side == null || side.isBlank()) {
                log.error("[ORDER-STATUS/REMOVE] ❌ Side is null/empty! orderId={}, ticker={}", 
                         orderId, ticker);
                return;
            }

            if ("BUY".equalsIgnoreCase(side)) {
                removeBidCompletely(ticker, price);
                log.info("[ORDER-STATUS/REMOVE] ✅ Removed BUY: orderId={}, ticker={}, price={}",
                        orderId, ticker, price);
            } else if ("SELL".equalsIgnoreCase(side)) {
                removeAskCompletely(ticker, price);
                log.info("[ORDER-STATUS/REMOVE] ✅ Removed SELL: orderId={}, ticker={}, price={}",
                        orderId, ticker, price);
            } else {
                log.error("[ORDER-STATUS/REMOVE] ❌ Unknown order side: orderId={}, side={}", 
                        orderId, side);
            }

        } catch (Exception e) {
            log.error("[ORDER-STATUS/REMOVE] ❌ Exception occurred: orderId={}, error={}", 
                    orderStatus.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * 주문 상태 이벤트 기반 호가 수량 업데이트
     * MARKET_PARTIAL 상태일 때 호출 (부분 체결)
     * 
     * ✅ v2.0 NULL 방어 로직 강화:
     * - remaining이 null이면 에러 로그 + 호가 제거
     * - 가격 불일치 시 경고 로그 출력
     */
    public void updateOrderQuantity(com.beyond.MKX.domain.orderbook.dto.OrderStatusEventDTO orderStatus) {
        try {
            String ticker = orderStatus.getTicker();
            String orderId = orderStatus.getOrderId();
            String side = orderStatus.getSide();
            long price = orderStatus.getPrice();
            BigDecimal remaining = orderStatus.getRemaining();

            // ✅ NULL 방어 로직 1: remaining이 null인 경우
            if (remaining == null) {
                log.error("[ORDER-STATUS/UPDATE] ❌ CRITICAL: Remaining is NULL! " +
                         "orderId={}, ticker={}, side={}, price={}. " +
                         "This indicates a data integrity issue. Removing order from book.",
                         orderId, ticker, side, price);
                
                // 임시 조치: 해당 가격의 호가 전체 제거 (데이터 불일치 방지)
                removeOrderFromBook(orderStatus);
                
                // 모니터링을 위한 추가 로그
                log.error("[ORDER-STATUS/UPDATE] ❌ Full orderStatus data: {}", orderStatus);
                return;
            }

            // ✅ NULL 방어 로직 2: remaining이 음수인 경우
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                log.error("[ORDER-STATUS/UPDATE] ❌ CRITICAL: Remaining is NEGATIVE! " +
                         "orderId={}, remaining={}. Setting to ZERO and removing.",
                         orderId, remaining);
                remaining = BigDecimal.ZERO;
            }

            log.info("[ORDER-STATUS/UPDATE] Updating quantity: orderId={}, ticker={}, side={}, price={}, remaining={}", 
                    orderId, ticker, side, price, remaining);

            // 남은 수량이 0이면 제거, 아니면 수량 업데이트
            if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                removeOrderFromBook(orderStatus);
                log.info("[ORDER-STATUS/UPDATE] ✅ Fully filled, removed: orderId={}", orderId);
            } else {
                // ✅ NULL 방어 로직 3: 호가 업데이트 전 기존 호가 존재 여부 확인
                OrderBook orderBook = getOrderBook(ticker);
                boolean existsInBook = false;
                
                if ("BUY".equalsIgnoreCase(side)) {
                    existsInBook = orderBook.getBids().stream()
                            .anyMatch(e -> e.getPrice() == price);
                    
                    if (!existsInBook) {
                        log.warn("[ORDER-STATUS/UPDATE] ⚠️ BID not found at price {}. " +
                                "Possible reasons: 1) Price changed due to market simulation, " +
                                "2) Already fully executed, 3) Race condition. " +
                                "Adding new entry instead of updating. orderId={}",
                                price, orderId);
                        // 호가가 없으면 새로 추가
                        addBid(ticker, price, remaining);
                    } else {
                        updateBidQuantity(ticker, price, remaining);
                    }
                } else if ("SELL".equalsIgnoreCase(side)) {
                    existsInBook = orderBook.getAsks().stream()
                            .anyMatch(e -> e.getPrice() == price);
                    
                    if (!existsInBook) {
                        log.warn("[ORDER-STATUS/UPDATE] ⚠️ ASK not found at price {}. " +
                                "Possible reasons: 1) Price changed due to market simulation, " +
                                "2) Already fully executed, 3) Race condition. " +
                                "Adding new entry instead of updating. orderId={}",
                                price, orderId);
                        // 호가가 없으면 새로 추가
                        addAsk(ticker, price, remaining);
                    } else {
                        updateAskQuantity(ticker, price, remaining);
                    }
                } else {
                    log.error("[ORDER-STATUS/UPDATE] ❌ Unknown order side: orderId={}, side={}", 
                            orderId, side);
                }
            }

        } catch (Exception e) {
            log.error("[ORDER-STATUS/UPDATE] ❌ Exception occurred while updating order quantity: " +
                     "orderId={}, error={}", 
                     orderStatus.getOrderId(), e.getMessage(), e);
        }
    }
    
    // ✅ 제거: WebSocket 의존성 제거로 인해 삭제
    // WebSocket 세션 수는 WebSocketHandler에서 직접 조회해야 함
}
