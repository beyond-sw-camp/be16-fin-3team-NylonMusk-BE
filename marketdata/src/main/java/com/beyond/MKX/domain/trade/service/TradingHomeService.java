package com.beyond.MKX.domain.trade.service;

import com.beyond.MKX.domain.price.entity.CurrentPrice;
import com.beyond.MKX.domain.price.service.CurrentPriceService;
import com.beyond.MKX.domain.trade.dto.StockBriefDTO;
import com.beyond.MKX.domain.trade.dto.TradingHomeItemResDTO;
import com.beyond.MKX.domain.trade.dto.TradingItemDetailResDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingHomeService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CurrentPriceService currentPriceService;
    private final StockMetaService stockMetaService;

    private static final String TURNOVER_KEY_PREFIX = "turnover:global:"; // yyyyMMdd

    public List<TradingHomeItemResDTO> getTopByTurnoverWithMeta(int limit, ZoneId zone) {
        String zkey = "turnover:global:" + LocalDate.now(zone).format(DateTimeFormatter.BASIC_ISO_DATE);
        Set<ZSetOperations.TypedTuple<Object>> top =
                redisTemplate.opsForZSet().reverseRangeWithScores(zkey, 0, Math.max(0, limit - 1));
        if (top == null || top.isEmpty()) return List.of();

        List<String> tickers = top.stream().map(t -> String.valueOf(t.getValue())).toList();
        Map<String, StockBriefDTO> metaMap = stockMetaService.fetchBriefMap(tickers);

        List<TradingHomeItemResDTO> list = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> e : top) {
            String ticker = String.valueOf(e.getValue());
            long turnover = e.getScore() == null ? 0L : e.getScore().longValue();

            CurrentPrice cp = currentPriceService.getCurrentPrice(ticker);
            long price = (cp != null) ? cp.getPrice() : 0L;
            BigDecimal rate = (cp != null && cp.getChangeRate() != null) ? cp.getChangeRate() : BigDecimal.ZERO;

            StockBriefDTO brief = metaMap.get(ticker);

            list.add(TradingHomeItemResDTO.builder()
                    .id(brief != null ? brief.getId() : null)
                    .ticker(ticker)
                    .name(brief != null ? brief.getNameKo() : ticker)
                    .currentPrice(price)
                    .changeRate(rate)
                    .turnover(turnover)
                    .status(brief != null ? brief.getStatus() : null)
                    .delistingStage(brief != null ? brief.getDelistingStage() : null)
                    .imageUrl(brief != null ? brief.getImageUrl() : null)
                    .build());
        }
        return list;
    }


    public List<TradingItemDetailResDTO> getTickerDetail(List<String> tickerList) {

        List<TradingItemDetailResDTO> list = new ArrayList<>();
        for (String ticker : tickerList) {
            CurrentPrice cp = currentPriceService.getCurrentPrice(ticker);
            long price = (cp != null) ? cp.getPrice() : 0L;
            BigDecimal rate = (cp != null && cp.getChangeRate() != null) ? cp.getChangeRate() : BigDecimal.ZERO;

            list.add(TradingItemDetailResDTO.builder()
                    .ticker(ticker)
                    .currentPrice(price)
                    .changeRate(rate)
                    .build());
        }

        return list;
    }
}
