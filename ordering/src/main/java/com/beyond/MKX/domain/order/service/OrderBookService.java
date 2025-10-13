package com.beyond.MKX.domain.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class OrderBookService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ZSetOperations<String, String> zSetOperations;

    public OrderBookService(@Qualifier("orderBookRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.zSetOperations = redisTemplate.opsForZSet();
    }

    private static final long PRICE_ADJUSTMENT_FACTOR = 1_000_000L;

    // 최고 매수가 조회
    public Optional<Long> getHighestBid(String ticker) {
        String key = "orderbook:{" + ticker + "}:bids";
        Set<TypedTuple<String>> typedTuples = zSetOperations.reverseRangeWithScores(key, 0, 0);

        if (CollectionUtils.isEmpty(typedTuples)) {
            log.warn("{} 종목에 조회된 최고 매수가가 없습니다.", ticker);
            return Optional.empty();
        }

        TypedTuple<String> firstTuple = typedTuples.iterator().next();
        Double score = firstTuple.getScore();

        return Optional.ofNullable(score)
                .map(Double::longValue) // Double -> long 변환
                .map(longScore -> longScore / PRICE_ADJUSTMENT_FACTOR); // long 값 나누기
    }

    // 최저 매도가 조회
    public Optional<Long> getLowestAsk(String ticker) {
        String key = "orderbook:{" + ticker + "}:asks";
        Set<TypedTuple<String>> typedTuples = zSetOperations.rangeWithScores(key, 0, 0);

        if (CollectionUtils.isEmpty(typedTuples)) {
            log.warn("{} 종목에 조회된 최저 매도가가 없습니다.", ticker);
            return Optional.empty();
        }

        TypedTuple<String> firstTuple = typedTuples.iterator().next();
        Double score = firstTuple.getScore();

        return Optional.ofNullable(score)
                .map(Double::longValue) // Double -> long 변환
                .map(longScore -> longScore / PRICE_ADJUSTMENT_FACTOR); // long 값 나누기
    }


}
