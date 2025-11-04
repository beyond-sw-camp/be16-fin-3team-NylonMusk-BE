package com.beyond.MKX.domain.ranking.service;

import com.beyond.MKX.domain.price.entity.CurrentPrice;
import com.beyond.MKX.domain.ranking.dto.UpdateRedisMarketRank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketRankWriterService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis Key 상수
    private static final String RANK_VALUE_KEY = "market:rank:trading-value";      // 거래 대금
    private static final String RANK_VOLUME_KEY = "market:rank:trading-volume";    // 거래량
    private static final String RANK_CHANGE_RATE_KEY = "market:rank:change-rate";  // 동락률
    private static final String RANK_DETAIL_KEY = "market:rank:detail:";

    /**
     * 거래량/거래대금 랭킹 업데이트
     * 
     * 당일 기준 누적 집계 (체결 이벤트마다 incrementScore)
     * Redis Key: market:rank:trading-value{YYYYMMDD}, market:rank:trading-volume{YYYYMMDD}
     * 
     * @param dto 거래량/거래대금 정보
     */
    public void updateVolumeAndValueRank(UpdateRedisMarketRank dto) {
        String format = dto.getTradingDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        String valueZKey = RANK_VALUE_KEY + format;  // ex) market:rank:trading-value20251104
        String volumeZKey = RANK_VOLUME_KEY + format; // ex) market:rank:trading-volume20251104

        // 거래대금 누적 (incrementScore로 atomic 누적)
        redisTemplate.opsForZSet().incrementScore(valueZKey, dto.getTicker(), dto.getTradeValue());
        // 거래량 누적
        redisTemplate.opsForZSet().incrementScore(volumeZKey, dto.getTicker(), dto.getVolume());

        // TTL 설정 (2일 후 자동 삭제)
        redisTemplate.expire(valueZKey, Duration.ofDays(2));
        redisTemplate.expire(volumeZKey, Duration.ofDays(2));
        
        log.debug("[RANK] Updated trading rank: ticker={}, date={}, value={}, volume={}", 
                dto.getTicker(), dto.getTradingDate(), dto.getTradeValue(), dto.getVolume());
    }

    /**
     * 등락률 랭킹 업데이트
     * 
     * 전일대비 등락률 기준 (체결 이벤트마다 최신 값으로 덮어쓰기)
     * Redis Key: market:rank:change-rate{YYYYMMDD}
     * 
     * @param currentPrice 현재가 정보 (등락률 포함)
     * @param tradingDate 거래 날짜
     */
    public void updateChangeRateRank(CurrentPrice currentPrice, LocalDate tradingDate) {
        try {
            if (currentPrice.getChangeRate() == null) {
                return;
            }
            String ticker = currentPrice.getTicker();
            double changeRate = currentPrice.getChangeRate().doubleValue();
            String changeRateZKey = RANK_CHANGE_RATE_KEY + tradingDate.format(DateTimeFormatter.BASIC_ISO_DATE);

            // Sorted Set에 등락률 저장 (add로 덮어쓰기 - 최신 등락률로 갱신)
            redisTemplate.opsForZSet().add(changeRateZKey, ticker, changeRate);
            
            // TTL 설정 (2일 후 자동 삭제)
            redisTemplate.expire(changeRateZKey, Duration.ofDays(2));
            
            log.debug("[RANK] Updated change rate rank: ticker={}, date={}, rate={}%",
                    ticker, tradingDate, changeRate);

        } catch (Exception e) {
            log.error("[RANK] Failed to update change rate rank: ticker={}",
                    currentPrice.getTicker(), e);
            // 예외를 다시 던지지 않음 - 랭킹 실패가 메인 로직을 방해하지 않도록
        }
    }


}
