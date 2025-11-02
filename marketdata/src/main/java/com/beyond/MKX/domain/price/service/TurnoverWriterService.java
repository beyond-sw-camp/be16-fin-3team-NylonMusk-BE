package com.beyond.MKX.domain.price.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional
public class TurnoverWriterService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void addExecution(String ticker, long notional, LocalDate tradingDate) {
        String zkey = "turnover:global:" + tradingDate.format(DateTimeFormatter.BASIC_ISO_DATE); // 20251031
        Double v = redisTemplate.opsForZSet().incrementScore(zkey, ticker, notional);
        // 선택: TTL 하루+여유
        redisTemplate.expire(zkey, Duration.ofDays(2));
        System.out.println("(" + v + ") 종목 거래대금 리스트 레디스 추가 완료 - ticker: " + ticker + " notional: " +  notional + " tradingDate: " + tradingDate);
    }



}
