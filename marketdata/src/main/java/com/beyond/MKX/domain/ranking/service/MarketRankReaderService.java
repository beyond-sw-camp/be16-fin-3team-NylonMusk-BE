package com.beyond.MKX.domain.ranking.service;

import com.beyond.MKX.domain.price.entity.CurrentPrice;
import com.beyond.MKX.domain.price.service.CurrentPriceService;
import com.beyond.MKX.domain.ranking.client.PlatformClient;
import com.beyond.MKX.domain.ranking.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * 마켓 랭킹 조회 서비스
 * 
 * Redis Sorted Set에서 랭킹 데이터를 조회하고 상세 정보를 조합
 * - 등락률 랭킹
 * - 거래량 랭킹
 * - 거래대금 랭킹
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketRankReaderService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CurrentPriceService currentPriceService;
    private final StockMetaService stockMetaService;
    private final PlatformClient platformClient;

    // Redis Key 상수
    private static final String RANK_VALUE_KEY = "market:rank:trading-value:";      // 거래 대금
    private static final String RANK_VOLUME_KEY = "market:rank:trading-volume:";    // 거래량
    private static final String RANK_CHANGE_RATE_KEY = "market:rank:change-rate:";  // 등락률

    /**
     * 오늘 날짜의 등락률 상위 30개 조회 (상승률)
     */
    public List<MarketStockListResDTO> getTodayTop30BySoarChangeRate() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        return getTop30ByChangeRate(today, ChangeRateType.SOAR);
    }

    /**
     * 오늘 날짜의 등락률 하위 30개 조회 (하락률)
     */
    public List<MarketStockListResDTO> getTodayTop30ByDescentChangeRate() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        return getTop30ByChangeRate(today, ChangeRateType.DESCENT);
    }

    /**
     * 오늘 날짜의 거래대금 상위 30개 조회
     */
    public List<MarketStockListResDTO> getTodayTop30ByTradingValue() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        return getTop30ByTradingValue(today);
    }

    /**
     * 오늘 날짜의 거래량 상위 30개 조회
     */
    public List<MarketStockListResDTO> getTodayTop30ByTradingVolume() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        return getTop30ByTradingVolume(today);
    }

    /**
     * 특정 날짜의 등락률 랭킹 조회
     * 
     * @param date 조회할 날짜
     * @param type 상승률(SOAR) 또는 하락률(DESCENT)
     * @return 상위 30개 종목 리스트
     */
    public List<MarketStockListResDTO> getTop30ByChangeRate(LocalDate date, ChangeRateType type) {
        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = RANK_CHANGE_RATE_KEY + dateStr;

        // Redis Sorted Set에서 ticker 조회
        Set<Object> tickersObj;
        if (type == ChangeRateType.SOAR) {
            // 상승률: 내림차순 (높은 값부터)
            tickersObj = redisTemplate.opsForZSet().reverseRange(key, 0, 29);
        } else {
            // 하락률: 오름차순 (낮은 값부터)
            tickersObj = redisTemplate.opsForZSet().range(key, 0, 29);
        }

        if (tickersObj == null || tickersObj.isEmpty()) {
            log.warn("[RANK] No change rate ranking data found for date: {}", date);
            return Collections.emptyList();
        }

        // Object -> String 변환
        List<String> tickers = tickersObj.stream()
                .map(String::valueOf)
                .toList();

        // 종목 상세 정보 조회 및 조합
        return buildMarketStockList(tickers);
    }

    /**
     * 특정 날짜의 거래대금 랭킹 조회
     */
    public List<MarketStockListResDTO> getTop30ByTradingValue(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = RANK_VALUE_KEY + dateStr;

        Set<Object> tickersObj = redisTemplate.opsForZSet().reverseRange(key, 0, 29);
        
        if (tickersObj == null || tickersObj.isEmpty()) {
            log.warn("[RANK] No trading value ranking data found for date: {}", date);
            return Collections.emptyList();
        }

        // Object -> String 변환
        List<String> tickers = tickersObj.stream()
                .map(String::valueOf)
                .toList();

        return buildMarketStockList(tickers);
    }

    /**
     * 특정 날짜의 거래량 랭킹 조회
     */
    public List<MarketStockListResDTO> getTop30ByTradingVolume(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = RANK_VOLUME_KEY + dateStr;

        Set<Object> tickersObj = redisTemplate.opsForZSet().reverseRange(key, 0, 29);
        
        if (tickersObj == null || tickersObj.isEmpty()) {
            log.warn("[RANK] No trading volume ranking data found for date: {}", date);
            return Collections.emptyList();
        }

        // Object -> String 변환
        List<String> tickers = tickersObj.stream()
                .map(String::valueOf)
                .toList();

        return buildMarketStockList(tickers);
    }

    /**
     * ticker 리스트를 받아 MarketStockListResDTO 리스트 생성
     * 
     * 1. PlatformClient에서 종목 메타 정보 조회 (id, name, status, delistingStage, imageUrl, totalSharesOutstanding)
     * 2. Redis에서 현재가 정보 조회 (currentPrice, changeRate, volume)
     * 3. 시가총액 계산 (currentPrice × totalSharesOutstanding)
     * 4. MarketStockListResDTO 조합
     */
    private List<MarketStockListResDTO> buildMarketStockList(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 종목 메타 정보 조회 (PlatformClient)
        Map<String, StockBriefDTO> metaMap = stockMetaService.fetchBriefMap(tickers);

        List<MarketStockListResDTO> result = new ArrayList<>();

        for (String ticker : tickers) {
            try {
                // 2. 현재가 정보 조회 (Redis)
                CurrentPrice currentPrice = currentPriceService.getCurrentPrice(ticker);
                if (currentPrice == null) {
                    log.warn("[RANK] CurrentPrice not found for ticker: {}", ticker);
                    continue;
                }

                // 3. 종목 메타 정보
                StockBriefDTO meta = metaMap.get(ticker);
                if (meta == null) {
                    log.warn("[RANK] StockBrief not found for ticker: {}", ticker);
                    continue;
                }

                // 4. 시가총액 계산 (currentPrice × totalSharesOutstanding)
                long marketCap = currentPrice.getPrice() * meta.getTotalSharesOutstanding();

                // 5. 거래량 (CurrentPrice의 volume을 long으로 변환)
                long tradingVolume = currentPrice.getVolume() != null 
                    ? currentPrice.getVolume().longValue() 
                    : 0L;

                // 6. DTO 조합
                MarketStockListResDTO dto = MarketStockListResDTO.builder()
                        .id(meta.getId())
                        .ticker(ticker)
                        .name(meta.getNameKo())
                        .status(meta.getStatus())
                        .delistingStage(meta.getDelistingStage())
                        .imageUrl(meta.getImageUrl())
                        .currentPrice(currentPrice.getPrice())
                        .changeRate(currentPrice.getChangeRate() != null 
                            ? currentPrice.getChangeRate() 
                            : BigDecimal.ZERO)
                        .tradingVolume(tradingVolume)
                        .marketCap(marketCap)
                        .build();

                result.add(dto);

            } catch (Exception e) {
                log.error("[RANK] Failed to build market stock for ticker: {}", ticker, e);
            }
        }

        log.info("[RANK] Built market stock list: {} items", result.size());
        return result;
    }

    // ========== 카드섹션 API ==========

    /**
     * 카드섹션 데이터 조회
     * 
     * 4개 섹션(인기/신규/상승률/거래량) 각각 TOP 3 종목 정보
     * 
     * @return CardSectionsResDTO (4개 섹션 데이터)
     */
    public CardSectionsResDTO getCardSections() {
        try {
            log.info("[CARD-SECTION] Fetching card section data");

            // 1. PlatformClient로 인기 + 신규 종목 한번에 조회
            CardSectionDataDTO platformData = platformClient.getCardSectionData(3);

            // 2. 인기 종목 카드 아이템 생성
            List<CardSectionItemDTO> popular = buildCardItems(platformData.getPopular());

            // 3. 신규 종목 카드 아이템 생성
            List<CardSectionItemDTO> newest = buildCardItems(platformData.getNewest());

            // 4. 상승률 TOP 3 (Redis에서 조회)
            List<CardSectionItemDTO> topChangeRate = getTop3ByChangeRate();

            // 5. 거래량 TOP 3 (Redis에서 조회)
            List<CardSectionItemDTO> topVolume = getTop3ByTradingVolume();

            CardSectionsResDTO result = CardSectionsResDTO.builder()
                    .popular(popular)
                    .newest(newest)
                    .topChangeRate(topChangeRate)
                    .topVolume(topVolume)
                    .build();

            log.info("[CARD-SECTION] Card section data retrieved: popular={}, newest={}, topChangeRate={}, topVolume={}", 
                    popular.size(), newest.size(), topChangeRate.size(), topVolume.size());

            return result;

        } catch (Exception e) {
            log.error("[CARD-SECTION] Failed to get card sections", e);
            return CardSectionsResDTO.builder()
                    .popular(Collections.emptyList())
                    .newest(Collections.emptyList())
                    .topChangeRate(Collections.emptyList())
                    .topVolume(Collections.emptyList())
                    .build();
        }
    }

    /**
     * 상승률 TOP 3 조회
     */
    private List<CardSectionItemDTO> getTop3ByChangeRate() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String dateStr = today.format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = RANK_CHANGE_RATE_KEY + dateStr;

        Set<Object> tickersObj = redisTemplate.opsForZSet().reverseRange(key, 0, 2); // TOP 3

        if (tickersObj == null || tickersObj.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tickers = tickersObj.stream()
                .map(String::valueOf)
                .toList();

        return buildCardItemsFromTickers(tickers);
    }

    /**
     * 거래량 TOP 3 조회
     */
    private List<CardSectionItemDTO> getTop3ByTradingVolume() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String dateStr = today.format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = RANK_VOLUME_KEY + dateStr;

        Set<Object> tickersObj = redisTemplate.opsForZSet().reverseRange(key, 0, 2); // TOP 3

        if (tickersObj == null || tickersObj.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tickers = tickersObj.stream()
                .map(String::valueOf)
                .toList();

        return buildCardItemsFromTickers(tickers);
    }

    /**
     * StockBriefDTO 리스트를 CardSectionItemDTO로 변환
     * 
     * CurrentPrice에서 현재가와 등락률 추가
     */
    private List<CardSectionItemDTO> buildCardItems(List<StockBriefDTO> briefs) {
        return briefs.stream()
                .map(brief -> {
                    CurrentPrice cp = currentPriceService.getCurrentPrice(brief.getTicker());
                    
                    return CardSectionItemDTO.builder()
                            .id(brief.getId())
                            .ticker(brief.getTicker())
                            .nameKo(brief.getNameKo())
                            .currentPrice(cp != null ? cp.getPrice() : 0)
                            .changeRate(cp != null && cp.getChangeRate() != null 
                                    ? cp.getChangeRate() 
                                    : BigDecimal.ZERO)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * ticker 리스트로부터 CardSectionItemDTO 생성
     * 
     * StockBrief 조회 + CurrentPrice 조합
     */
    private List<CardSectionItemDTO> buildCardItemsFromTickers(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Collections.emptyList();
        }

        // StockBrief 조회
        Map<String, StockBriefDTO> metaMap = stockMetaService.fetchBriefMap(tickers);

        return tickers.stream()
                .map(ticker -> {
                    StockBriefDTO brief = metaMap.get(ticker);
                    if (brief == null) {
                        return null;
                    }

                    CurrentPrice cp = currentPriceService.getCurrentPrice(ticker);

                    return CardSectionItemDTO.builder()
                            .id(brief.getId())
                            .ticker(ticker)
                            .nameKo(brief.getNameKo())
                            .currentPrice(cp != null ? cp.getPrice() : 0)
                            .changeRate(cp != null && cp.getChangeRate() != null 
                                    ? cp.getChangeRate() 
                                    : BigDecimal.ZERO)
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ========== 관심종목 API ==========

    /**
     * 사용자의 관심종목 마켓 데이터 조회
     * 
     * 1. PlatformClient로 사용자의 즐겨찾기 ticker 리스트 조회
     * 2. 각 ticker의 마켓 데이터 조회 (현재가, 등락률, 거래량, 시가총액)
     * 3. MarketStockListResDTO 리스트로 반환
     * 
     * @param memberId 회원 ID (헤더에서 추출)
     * @return 관심종목 마켓 데이터 리스트
     */
    public List<MarketStockListResDTO> getMyFavoriteStocks(UUID memberId) {
        try {
            log.info("[FAVORITES] Fetching favorite stocks for memberId: {}", memberId);

            // 1. PlatformClient로 즐겨찾기 ticker 리스트 조회
            List<String> tickers = platformClient.getFavoriteTickers(memberId);

            if (tickers == null || tickers.isEmpty()) {
                log.info("[FAVORITES] No favorite stocks found for memberId: {}", memberId);
                return Collections.emptyList();
            }

            log.info("[FAVORITES] Found {} favorite tickers for memberId: {}", tickers.size(), memberId);

            // 2. 각 ticker의 마켓 데이터 조회 및 조합
            List<MarketStockListResDTO> result = buildMarketStockList(tickers);

            log.info("[FAVORITES] Built {} market stock data for memberId: {}", result.size(), memberId);

            return result;

        } catch (Exception e) {
            log.error("[FAVORITES] Failed to get favorite stocks for memberId: {}", memberId, e);
            return Collections.emptyList();
        }
    }
}

