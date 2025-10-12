package com.beyond.MKX.domain.order.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderBookService {

    private final RedisTemplate<String, String> redisTemplate;

    public OrderBookService(@Qualifier("orderBookRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }



}
