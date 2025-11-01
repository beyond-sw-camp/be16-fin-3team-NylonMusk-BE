//package com.beyond.MKX.domain.trade.service;
//
//import com.beyond.MKX.domain.trade.client.PlatformClient;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class TickerNameService {
//
//    private final PlatformClient platformClient;
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    private static final String NAME_KEY_PREFIX = "ticker:name:"; // ticker:name:MKX001
//    private static final Duration NAME_TTL = Duration.ofDays(7);
//
//    public String getName(String ticker) {
//        String key = NAME_KEY_PREFIX + ticker;
//        Object cached = redisTemplate.opsForValue().get(key);
//        if (cached instanceof String s && !s.isEmpty()) return s;
//
//        String name = platformClient.getName(ticker);
//        if (name != null) {
//            redisTemplate.opsForValue().set(key, name, NAME_TTL);
//        }
//        return name;
//    }
//
//    public Map<String, String> getNamesBulk(List<String> tickers) {
//        Map<String, String> result = new HashMap<>();
//        List<String> miss = new ArrayList<>();
//
//        for (String t : tickers) {
//            String key = NAME_KEY_PREFIX + t;
//            Object cached = redisTemplate.opsForValue().get(key);
//            if (cached instanceof String s && !s.isEmpty()) {
//                result.put(t, s);
//            } else {
//                miss.add(t);
//            }
//        }
//
//        if (!miss.isEmpty()) {
//            Map<String, String> fetched = platformClient.getNames(miss);
//            if (fetched != null) {
//                fetched.forEach((t, name) -> {
//                    result.put(t, name);
//                    if (name != null) {
//                        redisTemplate.opsForValue().set(NAME_KEY_PREFIX + t, name, NAME_TTL);
//                    }
//                });
//            }
//        }
//        return result;
//    }
//}
