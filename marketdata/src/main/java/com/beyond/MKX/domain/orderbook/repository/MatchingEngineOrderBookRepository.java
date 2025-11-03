package com.beyond.MKX.domain.orderbook.repository;

import com.beyond.MKX.domain.orderbook.entity.OrderBook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Matching Engine의 Redis에서 Orderbook을 조회하는 Repository
 * 
 * matching-engine이 관리하는 Redis Cluster의 orderbook 데이터를
 * Lua Script를 통해 원자적으로 조회
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MatchingEngineOrderBookRepository {

    @Qualifier("matchingEngineRedisTemplate")
    private final StringRedisTemplate redisTemplate;
    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> getOrderBookScript;

    private static final int DEFAULT_MAX_DEPTH = 20;

    /**
     * Redis key 생성 (matching-engine과 동일한 형식)
     * hash tag 사용: {ticker}로 클러스터 슬롯 일관성 유지
     */
    private String orderBookKey(String ticker, String side) {
        final String sideKey = side.equalsIgnoreCase("BUY") ? "bids" : "asks";
        return "orderbook:{" + ticker + "}:" + sideKey;
    }

    /**
     * Orderbook 조회 (Lua Script 사용)
     * 
     * @param ticker 종목 코드
     * @param maxDepth 최대 조회 깊이 (기본 20)
     * @return OrderBook 엔티티
     */
    public OrderBook getOrderBook(String ticker, int maxDepth) {
        try {
            String bidsKey = orderBookKey(ticker, "BUY");
            String asksKey = orderBookKey(ticker, "SELL");

            log.info("[MATCHING-ENGINE-OB] 🔍 조회 시작: ticker={}, bidsKey={}, asksKey={}", ticker, bidsKey, asksKey);

            List<String> keys = Arrays.asList(bidsKey, asksKey);
            List<String> args = Arrays.asList(ticker, String.valueOf(maxDepth));

            @SuppressWarnings("unchecked")
            List<Object> rawResult = (List<Object>) redisTemplate.execute(getOrderBookScript, keys, args.toArray());

            log.info("[MATCHING-ENGINE-OB] 📊 Lua 스크립트 실행 결과: ticker={}, resultSize={}, maxDepth={}", 
                    ticker, rawResult != null ? rawResult.size() : 0, maxDepth);
            
            if (rawResult != null && !rawResult.isEmpty()) {
                // 가격 정보만 추출하여 로깅 (더 명확한 디버깅)
                List<String> prices = new ArrayList<>();
                boolean isAsk = false;
                for (int i = 0; i < Math.min(rawResult.size(), 40); i++) {
                    String str = String.valueOf(rawResult.get(i));
                    if ("ASKS".equals(str)) {
                        isAsk = true;
                        continue;
                    }
                    if (!isAsk && i % 2 == 0) {
                        // bids의 가격
                        try {
                            long price = Long.parseLong(str);
                            prices.add("BID:" + price);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    } else if (isAsk && i % 2 == 0) {
                        // asks의 가격
                        try {
                            long price = Long.parseLong(str);
                            prices.add("ASK:" + price);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
                log.info("[MATCHING-ENGINE-OB] 📋 조회된 호가 가격대 (처음 20개): {}", 
                    String.join(", ", prices));
            }

            if (rawResult == null || rawResult.isEmpty()) {
                log.warn("[MATCHING-ENGINE-OB] ⚠️ No orderbook data found: ticker={}", ticker);
                return OrderBook.createEmpty(ticker);
            }

            // Lua Script 결과 파싱
            // 형식: [bid_price1, bid_qty1, bid_price2, bid_qty2, ..., "ASKS", ask_price1, ask_qty1, ...]
            List<OrderBook.OrderBookEntry> bids = new ArrayList<>();
            List<OrderBook.OrderBookEntry> asks = new ArrayList<>();

            // ASKS 구분자 위치 찾기
            int asksSeparatorIndex = -1;
            for (int j = 0; j < rawResult.size(); j++) {
                if ("ASKS".equals(String.valueOf(rawResult.get(j)))) {
                    asksSeparatorIndex = j;
                    break;
                }
            }

            // bids 파싱 (ASKS 전까지)
            for (int i = 0; i < rawResult.size(); i++) {
                String str = String.valueOf(rawResult.get(i));
                
                if ("ASKS".equals(str)) {
                    break; // bids 파싱 종료
                }

                try {
                    // bids 처리: 짝수 인덱스=가격, 홀수 인덱스=수량
                    // 실제 원본 배열의 인덱스를 기준으로 판단해야 함 (ASKS 전까지의 원본 인덱스)
                    int bidsIndex = i; // 원본 배열의 인덱스 사용
                    if (bidsIndex % 2 == 0) {
                        // 짝수 번째 → 가격
                        long price = Long.parseLong(str);
                        bids.add(new OrderBook.OrderBookEntry(price, null));
                        log.debug("[MATCHING-ENGINE-OB] Added bid price: {}, total bids: {}", price, bids.size());
                    } else {
                        // 홀수 번째 → 수량 (마지막 bid에 수량 설정)
                        BigDecimal quantity = new BigDecimal(str);
                        if (!bids.isEmpty()) {
                            bids.get(bids.size() - 1).setQuantity(quantity);
                            log.debug("[MATCHING-ENGINE-OB] Set bid quantity: {}, for price: {}", 
                                quantity, bids.get(bids.size() - 1).getPrice());
                        } else {
                            log.warn("[MATCHING-ENGINE-OB] ⚠️ Cannot set quantity {} - no bid entry exists", quantity);
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("[MATCHING-ENGINE-OB] Failed to parse bid: {}, ticker={}", str, ticker);
                }
            }

            // asks 파싱 (ASKS 이후)
            if (asksSeparatorIndex >= 0) {
                for (int i = asksSeparatorIndex + 1; i < rawResult.size(); i++) {
                    String str = String.valueOf(rawResult.get(i));
                    
                    try {
                        // asks 처리: 짝수 인덱스=가격, 홀수 인덱스=수량 (ASKS 이후의 상대 인덱스)
                        int asksRelativeIndex = i - (asksSeparatorIndex + 1);
                        if (asksRelativeIndex % 2 == 0) {
                            // 짝수 번째 → 가격
                            long price = Long.parseLong(str);
                            asks.add(new OrderBook.OrderBookEntry(price, null));
                            log.debug("[MATCHING-ENGINE-OB] Added ask price: {}, total asks: {}", price, asks.size());
                        } else {
                            // 홀수 번째 → 수량 (마지막 ask에 수량 설정)
                            BigDecimal quantity = new BigDecimal(str);
                            if (!asks.isEmpty()) {
                                asks.get(asks.size() - 1).setQuantity(quantity);
                                log.debug("[MATCHING-ENGINE-OB] Set ask quantity: {}, for price: {}", 
                                    quantity, asks.get(asks.size() - 1).getPrice());
                            } else {
                                log.warn("[MATCHING-ENGINE-OB] ⚠️ Cannot set quantity {} - no ask entry exists", quantity);
                            }
                        }
                    } catch (NumberFormatException e) {
                        log.warn("[MATCHING-ENGINE-OB] Failed to parse ask: {}, ticker={}", str, ticker);
                    }
                }
            }

            log.info("[MATCHING-ENGINE-OB] 📦 파싱 전: ticker={}, bids={}, asks={}", ticker, bids.size(), asks.size());
            
            // 파싱된 모든 항목의 상세 정보 로그 (최대 50개만 출력)
            int maxLogEntries = Math.min(bids.size(), 50);
            for (int idx = 0; idx < maxLogEntries; idx++) {
                OrderBook.OrderBookEntry entry = bids.get(idx);
                log.info("[MATCHING-ENGINE-OB] 📋 Bid[{}]: price={}, quantity={}", idx, entry.getPrice(), entry.getQuantity());
            }
            if (bids.size() > 50) {
                log.info("[MATCHING-ENGINE-OB] ... (총 {}개 중 처음 50개만 표시)", bids.size());
            }
            
            maxLogEntries = Math.min(asks.size(), 50);
            for (int idx = 0; idx < maxLogEntries; idx++) {
                OrderBook.OrderBookEntry entry = asks.get(idx);
                log.info("[MATCHING-ENGINE-OB] 📋 Ask[{}]: price={}, quantity={}", idx, entry.getPrice(), entry.getQuantity());
            }
            if (asks.size() > 50) {
                log.info("[MATCHING-ENGINE-OB] ... (총 {}개 중 처음 50개만 표시)", asks.size());
            }
            
            // 수량이 null인 항목 제거
            bids = bids.stream()
                    .filter(e -> {
                        boolean valid = e.getQuantity() != null && e.getQuantity().compareTo(BigDecimal.ZERO) > 0;
                        if (!valid && e.getQuantity() == null) {
                            log.warn("[MATCHING-ENGINE-OB] ⚠️ Bid with null quantity removed: price={}", e.getPrice());
                        }
                        return valid;
                    })
                    .collect(Collectors.toList());
            asks = asks.stream()
                    .filter(e -> {
                        boolean valid = e.getQuantity() != null && e.getQuantity().compareTo(BigDecimal.ZERO) > 0;
                        if (!valid && e.getQuantity() == null) {
                            log.warn("[MATCHING-ENGINE-OB] ⚠️ Ask with null quantity removed: price={}", e.getPrice());
                        }
                        return valid;
                    })
                    .collect(Collectors.toList());

            log.info("[MATCHING-ENGINE-OB] 📦 파싱 후 필터링: ticker={}, bids={}, asks={}", ticker, bids.size(), asks.size());
            
            // ✅ 가격 순서 재정렬 보장 (Lua 스크립트에서 이미 정렬했지만, Java에서도 재정렬하여 보장)
            bids.sort((a, b) -> Long.compare(b.getPrice(), a.getPrice())); // 내림차순 (높은 가격 먼저)
            asks.sort((a, b) -> Long.compare(a.getPrice(), b.getPrice())); // 오름차순 (낮은 가격 먼저)
            
            if (!bids.isEmpty()) {
                log.info("[MATCHING-ENGINE-OB] 📋 Bid 가격대 요약: 총 {}개, 최고가={}, 최저가={}", 
                    bids.size(), 
                    bids.get(0).getPrice(), 
                    bids.get(bids.size() - 1).getPrice());
                log.info("[MATCHING-ENGINE-OB] 📋 첫 번째 Bid 샘플: price={}, quantity={}", 
                    bids.get(0).getPrice(), bids.get(0).getQuantity());
            } else {
                log.warn("[MATCHING-ENGINE-OB] ⚠️ Bid가 비어있습니다! Redis에서 orderId는 조회되었지만 priceInt 조회 실패 가능성");
            }
            if (!asks.isEmpty()) {
                log.info("[MATCHING-ENGINE-OB] 📋 Ask 가격대 요약: 총 {}개, 최저가={}, 최고가={}", 
                    asks.size(), 
                    asks.get(0).getPrice(), 
                    asks.get(asks.size() - 1).getPrice());
                log.info("[MATCHING-ENGINE-OB] 📋 첫 번째 Ask 샘플: price={}, quantity={}", 
                    asks.get(0).getPrice(), asks.get(0).getQuantity());
            } else {
                log.warn("[MATCHING-ENGINE-OB] ⚠️ Ask가 비어있습니다! Redis에서 orderId는 조회되었지만 priceInt 조회 실패 가능성");
            }

            OrderBook orderBook = OrderBook.builder()
                    .ticker(ticker)
                    .bids(bids)
                    .asks(asks)
                    .timestamp(System.currentTimeMillis())
                    .build();

            log.info("[MATCHING-ENGINE-OB] ✅ Retrieved orderbook: ticker={}, bids={}, asks={}", 
                    ticker, bids.size(), asks.size());

            return orderBook;

        } catch (Exception e) {
            log.error("[MATCHING-ENGINE-OB] ❌ Failed to get orderbook from matching-engine Redis: ticker={}", 
                    ticker, e);
            e.printStackTrace();
            return OrderBook.createEmpty(ticker);
        }
    }

    /**
     * Orderbook 조회 (기본 깊이 사용)
     */
    public OrderBook getOrderBook(String ticker) {
        return getOrderBook(ticker, DEFAULT_MAX_DEPTH);
    }
}

