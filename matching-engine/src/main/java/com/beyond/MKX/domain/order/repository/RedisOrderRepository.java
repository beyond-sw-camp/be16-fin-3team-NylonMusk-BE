package com.beyond.MKX.domain.order.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class RedisOrderRepository {

    private final StringRedisTemplate redisTemplate;

    /**
     * 기존 marketMatchScript 이름을 재사용한다.
     * (Bean 구성에서 이 스크립트에 "match_and_maybe_add.lua"를 로딩하도록만 바꿔주면 됨)
     *
     * KEYS: [ orderbook:{ticker}:bids, orderbook:{ticker}:asks, seq:{ticker}:bids, seq:{ticker}:asks ]
     * ARGV: [ ticker, side, qty, maxMatches, guardPriceInt, orderIdToAdd, priceIntForAdd, FACTOR ]
     */
    private final DefaultRedisScript<List> marketMatchScript;

    // 가격·시간 분리용 (Lua와 동일 값 사용)
    private static final long FACTOR = 1_000_000L;

    // ---- Key builders (해시태그 일관성 유지) ----
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

    // ---- KRW 정수화/복원 ----
    private long toPriceInt(double price) {
        return BigDecimal.valueOf(price).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private double fromPriceInt(long priceInt) {
        return BigDecimal.valueOf(priceInt).doubleValue();
    }

    /*
    FACTOR - seq 오버런(백만 건 이후 역전 위험)
    seq가 FACTOR(1,000,000)를 넘으면 (FACTOR - seq)가 음수가 되어 가격 동률 내 시간 우선이 뒤집힐 수 있습니다.
    → mod 연산으로 안전화하세요.
     */
    private double bidScore(long priceInt, long seq) {
        long s = seq % FACTOR;                    // 시간 tie-breaker 안전화
        return (double) (priceInt * FACTOR + (FACTOR - s));
    }

    private double askScore(long priceInt, long seq) {
        long s = seq % FACTOR;
        return (double) (priceInt * FACTOR + s);
    }

    // ---- 신규 주문 적재 (단순 적재용; 지정가 잔량은 보통 Lua가 적재) ----
    public void addOrder(String ticker, String orderId, double price, double quantity, String side) {
        final String bookKey   = orderBookKey(ticker, side);
        final String detailKey = orderDetailKey(ticker, orderId);
        final String idxKey    = orderIndexKey(orderId);

        final long priceInt = toPriceInt(price);
        final Long seqBoxed = redisTemplate.opsForValue().increment(seqKey(ticker, side));
        final long seq = (seqBoxed != null) ? seqBoxed : 1L;
        final double score = side.equalsIgnoreCase("BUY") ? bidScore(priceInt, seq) : askScore(priceInt, seq);

        redisTemplate.opsForZSet().add(bookKey, orderId, score);

        Map<String, String> detail = new HashMap<>();
        detail.put("quantity", String.valueOf(quantity));
        detail.put("priceInt", String.valueOf(priceInt));
        detail.put("side", side);
        detail.put("ticker", ticker);
        redisTemplate.opsForHash().putAll(detailKey, detail);

        Map<String, String> idx = new HashMap<>();
        idx.put("ticker", ticker);
        idx.put("side", side);
        redisTemplate.opsForHash().putAll(idxKey, idx);
    }

    // ---- 취소 ----
    public void cancelOrder(String orderId, String ticker, String side) {
        final String bookKey = orderBookKey(ticker, side);
        redisTemplate.opsForZSet().remove(bookKey, orderId);
        redisTemplate.delete(orderDetailKey(ticker, orderId));
        redisTemplate.delete(orderIndexKey(orderId));
    }

    // ---- 탑오더 조회(점검) ----
    public String getTopOrder(String ticker, boolean isBuy) {
        final String key = "orderbook:{" + ticker + "}:" + (isBuy ? "bids" : "asks");
        Set<String> r = isBuy ? redisTemplate.opsForZSet().reverseRange(key, 0, 0)
                : redisTemplate.opsForZSet().range(key, 0, 0);
        if (r == null || r.isEmpty()) return null;
        return r.iterator().next();
    }

    // ---- 시장가/지정가 공용: 가격 가드 + (지정가 잔량 적재) ----
    public MatchResult matchOrAddLimit(
            String ticker, String side,
            double quantity, int maxMatches,
            Long guardPriceInt,           // 시장가도 가드 사용 (null이면 무제한으로 보고 큰 값/작은 값으로 대체 가능)
            String orderIdToAdd,          // 지정가면 주문ID, 시장가면 null
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

        double remaining = Double.parseDouble(raw.get(0).toString());
        int count = Integer.parseInt(raw.get(1).toString());

        List<TradeFill> fills = new ArrayList<>(count);
        int i = 2;
        for (int k = 0; k < count && i + 2 < raw.size(); k++) {
            String counterOrderId = raw.get(i++).toString();
            double fillQty        = Double.parseDouble(raw.get(i++).toString());
            long priceInt         = Long.parseLong(raw.get(i++).toString());
            double price          = fromPriceInt(priceInt);
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

    public boolean ensureLimitOrderPresent(
            String ticker, String orderId, String side, Double price, Double remainingQty) {

        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(remainingQty, "remainingQty");

        final String detailKey = orderDetailKey(ticker, orderId);
        Boolean exists = redisTemplate.hasKey(detailKey);
        if (exists) return false;

        addOrder(ticker, orderId, price, remainingQty, side);
        return true;
    }
    // ---- 외부에서 쓰기 편하도록 KRW 가격→정수 변환 도우미 노출 ----
    public long asIntPrice(double price) { return toPriceInt(price); }

    // ---- DTO ----
    public record TradeFill(String counterOrderId, double quantity, double price) {}
    public record MatchResult(double remaining, List<TradeFill> fills, boolean addedToBook) {}
}
