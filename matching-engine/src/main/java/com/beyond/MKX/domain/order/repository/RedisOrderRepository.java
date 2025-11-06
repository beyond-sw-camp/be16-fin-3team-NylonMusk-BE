package com.beyond.MKX.domain.order.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Redis 기반 오더북 저장소.
 *
 * 구성 개요
 * - 오더북 ZSET
 *     - key: orderbook:{ticker}:bids | orderbook:{ticker}:asks
 *     - score: 가격·시간을 합친 우선순위 점수(가격 우선, 시간 동순위)
 * - 주문 상세 HASH
 *     - key: orders:{ticker}:{orderId}   (동일 {ticker} 해시태그 필수)
 *     - fields: quantity, priceInt, side, ticker
 * - 인덱스 HASH
 *     - key: orderIndex:{orderId}        (ticker/side 빠른 역참조)
 *
 * 매칭
 * - Lua 스크립트(marketMatchScript) 호출로 시장가/지정가 공통 매칭.
 * - ARGV/KEYS 프로토콜은 matchOrAddLimit(...) Javadoc 참조.
 */
@Repository
@RequiredArgsConstructor
public class RedisOrderRepository {

    private final StringRedisTemplate redisTemplate;

    /**
     * 매칭 Lua 스크립트.
     *
     * KEYS:
     *  1) orderbook:{ticker}:bids
     *  2) orderbook:{ticker}:asks
     *  3) seq:{ticker}:bids
     *  4) seq:{ticker}:asks
     *
     * ARGV:
     *  1) ticker               : 종목 코드
     *  2) side                 : "BUY" | "SELL" (들어온 주문의 사이드)
     *  3) qty                  : 주문 수량(double 문자열)
     *  4) maxMatches           : 한 번의 실행에서 소화할 최대 매칭 건수
     *  5) guardPriceInt        : 가격 가드(시장가 안전장치 / 지정가 한계가격). null이면 0
     *  6) orderIdToAdd         : 지정가일 때 잔량 적재에 사용할 orderId. 시장가는 빈 문자열
     *  7) priceIntForAdd       : 지정가 잔량 적재 가격(정수). 시장가는 0
     *  8) FACTOR               : 가격·시간 분리용 스케일(동일 값 유지)
     */
    private final DefaultRedisScript<List> marketMatchScript;

    /** 가격·시간 tie-breaker 스케일 (Lua와 동일 값 유지) */
    private static final long FACTOR = 1_000_000L;

    // ----------------------------------------------------------------------
    // Key builders (반드시 {ticker} 해시태그 사용: 클러스터 슬롯 일관성)
    // ----------------------------------------------------------------------
    private String orderDetailKey(String ticker, String orderId) { return "orders:{" + ticker + "}:" + orderId; }
    private String orderIndexKey(String orderId) { return "orderIndex:{" + orderId + "}"; }

    private String orderBookKey(String ticker, String side) {
        final String sideKey = side.equalsIgnoreCase("BUY") ? "bids" : "asks";
        return "orderbook:{" + ticker + "}:" + sideKey;
    }
    private String seqKey(String ticker, String side) {
        final String sideKey = side.equalsIgnoreCase("BUY") ? "bids" : "asks";
        return "seq:{" + ticker + "}:" + sideKey;
    }
    
    /**
     * 총 호가 잔량 키 (Redis HASH)
     * key: orderbook:total:{ticker}
     * fields: bidVolume, askVolume
     */
    private String totalVolumeKey(String ticker) {
        return "orderbook:total:{" + ticker + "}";
    }

    // ----------------------------------------------------------------------
    // KRW 가격 정수화/복원 (정수 가격 저장 정책)
    // ----------------------------------------------------------------------
    private long toPriceInt(double price) {
        return BigDecimal.valueOf(price).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    // ----------------------------------------------------------------------
    // 점수 계산 (가격 우선, 동가격 내 시간 우선)
    //  - seq가 커져도 역전이 발생하지 않도록 FACTOR로 mod 처리
    // ----------------------------------------------------------------------
    private double bidScore(long priceInt, long seq) {
        long s = seq % FACTOR;                    // 시간 tie-breaker 안전화
        return (double) (priceInt * FACTOR + (FACTOR - s)); // 높은 가격·이른 시간 우선
    }

    private double askScore(long priceInt, long seq) {
        long s = seq % FACTOR;
        return (double) (priceInt * FACTOR + s); // 낮은 가격·이른 시간 우선
    }

    // ----------------------------------------------------------------------
    // 신규 주문 적재 (단순 적재용; 지정가 잔량은 일반적으로 Lua가 적재)
    // ----------------------------------------------------------------------
    public void addOrder(String ticker, String orderId, long priceInt, BigDecimal quantity, String side) {
        final String bookKey   = orderBookKey(ticker, side);
        final String detailKey = orderDetailKey(ticker, orderId);
        final String idxKey    = orderIndexKey(orderId);

        final Long seqBoxed = redisTemplate.opsForValue().increment(seqKey(ticker, side));
        final long seq = (seqBoxed != null) ? seqBoxed : 1L;
        final double score = side.equalsIgnoreCase("BUY") ? bidScore(priceInt, seq) : askScore(priceInt, seq);

        redisTemplate.opsForZSet().add(bookKey, orderId, score);

        Map<String, String> detail = new HashMap<>();
        detail.put("quantity", quantity.toPlainString());
        detail.put("priceInt", String.valueOf(priceInt));
        detail.put("side", side);
        detail.put("ticker", ticker);
        redisTemplate.opsForHash().putAll(detailKey, detail);

        Map<String, String> idx = new HashMap<>();
        idx.put("ticker", ticker);
        idx.put("side", side);
        redisTemplate.opsForHash().putAll(idxKey, idx);
        
        // ✅ 총 호가 잔량 증가
        incrementTotalVolume(ticker, side, quantity);
    }

    // ----------------------------------------------------------------------
    // 취소
    // ----------------------------------------------------------------------
    public void cancelOrder(String orderId, String ticker, String side) {
        final String bookKey = orderBookKey(ticker, side);
        final String detailKey = orderDetailKey(ticker, orderId);
        
        // 주문 수량 조회 (총량 감소용)
        String quantityStr = (String) redisTemplate.opsForHash().get(detailKey, "quantity");
        BigDecimal quantity = null;
        if (quantityStr != null) {
            try {
                quantity = new BigDecimal(quantityStr);
            } catch (NumberFormatException e) {
                // 수량 파싱 실패 시 무시
            }
        }
        
        redisTemplate.opsForZSet().remove(bookKey, orderId);
        redisTemplate.delete(detailKey);
        redisTemplate.delete(orderIndexKey(orderId));
        
        // ✅ 총 호가 잔량 감소
        if (quantity != null) {
            decrementTotalVolume(ticker, side, quantity);
        }
    }
    
    // ----------------------------------------------------------------------
    // 총 호가 잔량 관리
    // ----------------------------------------------------------------------
    
    /**
     * 총 호가 잔량 증가
     */
    public void incrementTotalVolume(String ticker, String side, BigDecimal quantity) {
        final String totalKey = totalVolumeKey(ticker);
        final String field = side.equalsIgnoreCase("BUY") ? "bidVolume" : "askVolume";
        redisTemplate.opsForHash().increment(totalKey, field, quantity.doubleValue());
    }
    
    /**
     * 총 호가 잔량 감소
     */
    public void decrementTotalVolume(String ticker, String side, BigDecimal quantity) {
        final String totalKey = totalVolumeKey(ticker);
        final String field = side.equalsIgnoreCase("BUY") ? "bidVolume" : "askVolume";
        redisTemplate.opsForHash().increment(totalKey, field, -quantity.doubleValue());
    }
    
    /**
     * 총 호가 잔량 조회
     */
    public BigDecimal getTotalVolume(String ticker, String side) {
        final String totalKey = totalVolumeKey(ticker);
        final String field = side.equalsIgnoreCase("BUY") ? "bidVolume" : "askVolume";
        Object value = redisTemplate.opsForHash().get(totalKey, field);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    // ----------------------------------------------------------------------
    // 탑오더 조회(점검/디버깅용)
    // ----------------------------------------------------------------------
    public String getTopOrder(String ticker, boolean isBuy) {
        final String key = "orderbook:{" + ticker + "}:" + (isBuy ? "bids" : "asks");
        Set<String> r = isBuy ? redisTemplate.opsForZSet().reverseRange(key, 0, 0)
                : redisTemplate.opsForZSet().range(key, 0, 0);
        if (r == null || r.isEmpty()) return null;
        return r.iterator().next();
    }

    // ----------------------------------------------------------------------
    // 시장가/지정가 공용 매칭 진입점
    //  - guardPriceInt: 가격 가드(시장가의 안전장치/지정가 한계가격)
    //  - orderIdToAdd / priceIntForAdd: 지정가일 때 잔량을 동일 ID/가격으로 적재
    // 반환: 남은 수량, 체결 목록, 잔량 적재 여부
    // ----------------------------------------------------------------------
    public MatchResult matchOrAddLimit(
            String ticker, String side,
            BigDecimal quantity, int maxMatches,
            Long guardPriceInt,           // 시장가도 가드 사용 (null이면 0으로 전달)
            String orderIdToAdd,          // 지정가면 주문ID, 시장가면 null/빈 문자열
            Long priceIntForAdd           // 지정가 적재 가격(보통 guardPriceInt와 같음)
    ) {
        String bidsKey = orderBookKey(ticker, "BUY");
        String asksKey = orderBookKey(ticker, "SELL");
        String seqBids = seqKey(ticker, "BUY");
        String seqAsks = seqKey(ticker, "SELL");

        List<String> keys = Arrays.asList(bidsKey, asksKey, seqBids, seqAsks);
        List<String> args = Arrays.asList(
                ticker,
                side,
                String.valueOf(quantity),
                String.valueOf(maxMatches),
                String.valueOf(guardPriceInt == null ? 0L : guardPriceInt),
                orderIdToAdd == null ? "" : orderIdToAdd,
                String.valueOf(priceIntForAdd == null ? 0L : priceIntForAdd),
                String.valueOf(FACTOR)
        );

        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) redisTemplate.execute(marketMatchScript, keys, args.toArray());

        if (raw == null || raw.size() < 2) {
            return new MatchResult(quantity, Collections.emptyList(), false);
        }

        String remainingStr = Objects.toString(raw.get(0), "0");
        BigDecimal remaining = new BigDecimal(remainingStr); // 문자열→BigDecimal 직파싱(정밀도 유지)

        int count = Integer.parseInt(raw.get(1).toString());

        List<TradeFill> fills = new ArrayList<>(count);
        int i = 2;
        for (int k = 0; k < count && i + 2 < raw.size(); k++) {
            String counterOrderId = raw.get(i++).toString();
            BigDecimal fillQty = new BigDecimal(raw.get(i++).toString());
            long price = Long.parseLong(raw.get(i++).toString());
            fills.add(new TradeFill(counterOrderId, fillQty, price));
        }

        boolean added = false;
        for (; i < raw.size() - 1; i++) {
            if ("ADDED".equals(raw.get(i).toString())) {
                added = "1".equals(raw.get(i + 1).toString());
                break;
            }
        }
        return new MatchResult(remaining, fills, added);
    }

    /**
     * 잔량 적재 보장(보강 루틴).
     * - Lua가 잔량 적재를 못 한 경우를 대비하여, 동일 orderId가 없을 때 addOrder 수행.
     *
     * @return 이미 존재하면 false, 새로 적재했으면 true
     */
    public boolean ensureLimitOrderPresent(
            String ticker, String orderId, String side, long price, BigDecimal remainingQty) {

        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(remainingQty, "remainingQty");

        final String detailKey = orderDetailKey(ticker, orderId);
        Boolean exists = redisTemplate.hasKey(detailKey);
        if (exists) return false;

        addOrder(ticker, orderId, price, remainingQty, side);
        return true;
    }

    // 외부 사용을 위한 가격→정수 변환 도우미
    public long asIntPrice(double price) { return toPriceInt(price); }

    // ----------------------------------------------------------------------
    // DTO
    // ----------------------------------------------------------------------
    public record TradeFill(String counterOrderId, BigDecimal quantity, long price) {}
    public record MatchResult(BigDecimal remaining, List<TradeFill> fills, boolean addedToBook) {}
}
