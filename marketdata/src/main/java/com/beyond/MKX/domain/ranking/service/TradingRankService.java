package com.beyond.MKX.domain.ranking.service;

import com.beyond.MKX.domain.execution.entity.Execution;
import com.beyond.MKX.domain.price.entity.CurrentPrice;
import com.beyond.MKX.domain.price.service.CurrentPriceService;
import com.beyond.MKX.domain.ranking.dto.ChangeRateType;
import com.beyond.MKX.domain.ranking.dto.TradingRankDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 거래대금/거래량 랭킹보드 서비스
 *
 * Redis Sorted Set(Z-Set)을 활용하여 실시간 랭킹 제공
 * - 거래대금 랭킹: market:rank:trading-value
 * - 거래량 랭킹: market:rank:trading-volume
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingRankService {

    private final InfluxDBClient influxDBClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${influx.bucket}")
    private String bucket;

    @Value("${influx.org}")
    private String organization;

    // Redis Key 상수
    private static final String RANK_VALUE_KEY = "market:rank:trading-value";
    private static final String RANK_VOLUME_KEY = "market:rank:trading-volume";
    private static final String RANK_DETAIL_KEY = "market:rank:detail:";
    private static final String RANK_CHANGE_RATE_KEY = "market:rank:change-rate";

    // 랭킹 보드 크기
    private static final int RANK_SIZE = 30;

    /**
     * 모든 종목의 24시간 거래대금/거래량 계산 및 Redis 업데이트
     *
     * 스케줄러에서 5초마다 호출됨
     */
    public void updateTradingRanks() {
        Instant now = Instant.now();
        Instant start = now.minus(24, ChronoUnit.HOURS);

        System.out.println("[RANK] 거래대금/거래량 랭킹 업데이트 시작 - period: "+ start +" ~ " + now);
        log.info("[RANK] 거래대금/거래량 랭킹 업데이트 시작 - period: {} ~ {}", start, now);

        try {
            // 모든 종목의 거래 데이터 조회
            Map<String, TradingStats> tradingStatsMap = fetchAllTradingStats(start, now);

            if (tradingStatsMap.isEmpty()) {
                System.out.println("[RANK] 조회된 거래 데이터가 없습니다.");
                log.warn("[RANK] 조회된 거래 데이터가 없습니다.");
                return;
            }

            // Redis에 랭킹 데이터 저장
            updateRedisRanks(tradingStatsMap, now, start);

            System.out.println("[RANK] 거래대금/거래량 랭킹 업데이트 완료 - {" + tradingStatsMap.size() + "} 종목 처리");
            log.info("[RANK] 거래대금/거래량 랭킹 업데이트 완료 - {} 종목 처리", tradingStatsMap.size());

        } catch (Exception e) {
            log.error("[RANK] 거래대금/거래량 랭킹 업데이트 실패", e);
        }
    }

    /// **-------------- 동락률 랭킹 설정 --------------**

    /**
     * 등락률 랭킹 업데이트
     */
    public void updateChangeRateRank(CurrentPrice currentPrice) {
        try {
            if (currentPrice.getChangeRate() == null) {
                return;
            }

            String ticker = currentPrice.getTicker();
            double changeRate = currentPrice.getChangeRate().doubleValue();

            // Sorted Set에 등락률 저장
            redisTemplate.opsForZSet().add(RANK_CHANGE_RATE_KEY, ticker, changeRate);

            log.debug("[RANK] Updated change rate rank: ticker={}, rate={}%",
                    ticker, changeRate);

        } catch (Exception e) {
            log.error("[RANK] Failed to update rank: ticker={}",
                    currentPrice.getTicker(), e);
            // 예외를 다시 던지지 않음 - 랭킹 실패가 메인 로직을 방해하지 않도록
        }
    }

    /**
     * 상승률 상위 30개 조회
     */
    public List<CurrentPrice> getTop30BySoarChangeRate() {
        return getTopNByChangeRate(30, ChangeRateType.SOAR);
    }

    /**
     * 하락률 상위 30개 조회
     */
    public List<CurrentPrice> getTop30ByDescentChangeRate() {
        return getTopNByChangeRate(30, ChangeRateType.DESCENT);
    }

    /**
     * 등락률 상위 N개 조회
     */
    public List<CurrentPrice> getTopNByChangeRate(int n, ChangeRateType changeRateType) {
        try {
            Set<String> topTickers = new HashSet<>();
            if (changeRateType == ChangeRateType.SOAR) {
                topTickers = redisTemplate.opsForZSet()
                        .reverseRange(RANK_CHANGE_RATE_KEY, 0, n - 1);
            } else if (changeRateType == ChangeRateType.DESCENT) {
                topTickers = redisTemplate.opsForZSet()
                        .range(RANK_CHANGE_RATE_KEY, 0, n - 1);
            }

            if (topTickers == null || topTickers.isEmpty()) {
                log.warn("[RANK] No change rate ranking data found");
                return Collections.emptyList();
            }

            // 각 ticker의 현재가 정보 조회
            List<CurrentPrice> result = new ArrayList<>();
            for (String ticker : topTickers) {
                CurrentPrice price = getCurrentPriceFromRedis(ticker);
                if (price != null) {
                    result.add(price);
                }
            }

            log.info("[RANK] Retrieved top {} by change rate: {} items", n, result.size());
            return result;

        } catch (Exception e) {
            log.error("[RANK] Failed to get top N by change rate", e);
            return Collections.emptyList();
        }
    }


    /// **-------------- 동락률 랭킹 설정 --------------**


    /**
     * InfluxDB에서 모든 종목의 거래 통계 조회
     *
     * @param start 시작 시각
     * @param end 종료 시각
     * @return 종목별 거래 통계 맵
     */
    private Map<String, TradingStats> fetchAllTradingStats(Instant start, Instant end) {
        // Flux 쿼리: 각 종목별 거래대금과 거래량 집계
        // ✅ 타입 변환 추가: toFloat()로 int/float 충돌 해결
        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r._measurement == \"%s\") " +
                        "|> filter(fn: (r) => r._field == \"price\" or r._field == \"quantity\") " +
                        "|> toFloat() " +  // ✅ 모든 값을 float로 변환
                        "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                        "|> group(columns: [\"ticker\"]) " +
                        "|> reduce( " +
                        "    identity: {tradingValue: 0.0, tradingVolume: 0.0}, " +
                        "    fn: (r, accumulator) => ({ " +
                        "        tradingValue: accumulator.tradingValue + (r.price * r.quantity), " +
                        "        tradingVolume: accumulator.tradingVolume + r.quantity " +
                        "    }) " +
                        ")",
                bucket, start.toString(), end.toString(), Execution.MEASUREMENT
        );

        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, organization);
            Map<String, TradingStats> statsMap = new HashMap<>();

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    String ticker = (String) record.getValueByKey("ticker");

                    // tradingValue와 tradingVolume 값 추출
                    Object valueObj = record.getValueByKey("tradingValue");
                    Object volumeObj = record.getValueByKey("tradingVolume");

                    if (ticker != null && valueObj != null && volumeObj != null) {
                        BigDecimal tradingValue = BigDecimal.valueOf(((Number) valueObj).doubleValue())
                                .setScale(2, RoundingMode.HALF_UP);
                        BigDecimal tradingVolume = BigDecimal.valueOf(((Number) volumeObj).doubleValue())
                                .setScale(4, RoundingMode.HALF_UP);

                        TradingStats stats = TradingStats.builder()
                                .ticker(ticker)
                                .tradingValue(tradingValue)
                                .tradingVolume(tradingVolume)
                                .build();

                        statsMap.put(ticker, stats);

                        log.debug("[RANK] {} - 거래대금: {}, 거래량: {}",
                                ticker, tradingValue, tradingVolume);
                    }
                }
            }

            log.info("[RANK] InfluxDB에서 {} 종목의 거래 통계 조회 완료", statsMap.size());
            return statsMap;

        } catch (Exception e) {
            log.error("[RANK] InfluxDB 거래 통계 조회 실패", e);
            return new HashMap<>();
        }
    }

    /**
     * Redis Sorted Set에 랭킹 데이터 저장
     *
     * @param statsMap 종목별 거래 통계
     * @param now 현재 시각
     * @param start 24시간 시작 시각
     */
    private void updateRedisRanks(Map<String, TradingStats> statsMap, Instant now, Instant start) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // 기존 랭킹 데이터 삭제
        redisTemplate.delete(RANK_VALUE_KEY);
        redisTemplate.delete(RANK_VOLUME_KEY);

        // 거래대금 랭킹 저장 (score = 거래대금)
        for (Map.Entry<String, TradingStats> entry : statsMap.entrySet()) {
            String ticker = entry.getKey();
            TradingStats stats = entry.getValue();

            // 거래대금 랭킹
            zSetOps.add(RANK_VALUE_KEY, ticker, stats.getTradingValue().doubleValue());

            // 거래량 랭킹
            zSetOps.add(RANK_VOLUME_KEY, ticker, stats.getTradingVolume().doubleValue());

            // 상세 정보 저장 (Hash)
            String detailKey = RANK_DETAIL_KEY + ticker;
            Map<String, String> detailMap = new HashMap<>();
            detailMap.put("tradingValue", stats.getTradingValue().toString());
            detailMap.put("tradingVolume", stats.getTradingVolume().toString());
            detailMap.put("updatedAt", now.toString());
            detailMap.put("startTime", start.toString());
            detailMap.put("endTime", now.toString());

            redisTemplate.opsForHash().putAll(detailKey, detailMap);
        }

        log.info("[RANK] Redis에 {} 종목의 랭킹 데이터 저장 완료", statsMap.size());
    }




    /**
     * 거래대금 TOP 30 조회
     *
     * @return 거래대금 TOP 30 리스트
     */
    public List<TradingRankDTO> getTop30ByTradingValue() {
        return getTopNByKey(RANK_VALUE_KEY, RANK_SIZE, true);
    }

    /**
     * 거래량 TOP 30 조회
     *
     * @return 거래량 TOP 30 리스트
     */
    public List<TradingRankDTO> getTop30ByTradingVolume() {
        return getTopNByKey(RANK_VOLUME_KEY, RANK_SIZE, false);
    }

    /**
     * Redis Sorted Set에서 TOP N 조회
     *
     * @param key Redis Key
     * @param n 조회할 개수
     * @param isValueRank 거래대금 랭킹 여부
     * @return TOP N 리스트
     */
    private List<TradingRankDTO> getTopNByKey(String key, int n, boolean isValueRank) {
        try {
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

            // 내림차순으로 TOP N 조회
            Set<ZSetOperations.TypedTuple<String>> topN =
                    zSetOps.reverseRangeWithScores(key, 0, n - 1);

            if (topN == null || topN.isEmpty()) {
                log.warn("[RANK] Redis에서 랭킹 데이터를 찾을 수 없습니다. key: {}", key);
                return Collections.emptyList();
            }

            List<TradingRankDTO> rankList = new ArrayList<>();
            int rank = 1;

            for (ZSetOperations.TypedTuple<String> tuple : topN) {
                String ticker = tuple.getValue();
                Double score = tuple.getScore();

                if (ticker == null || score == null) {
                    continue;
                }

                // Redis Hash에서 상세 정보 조회
                String detailKey = RANK_DETAIL_KEY + ticker;
                Map<Object, Object> detailMap = redisTemplate.opsForHash().entries(detailKey);

                TradingRankDTO dto = TradingRankDTO.builder()
                        .ticker(ticker)
                        .build();

                if (isValueRank) {
                    dto.setTradingValue(BigDecimal.valueOf(score));
                    dto.setValueRank(rank);

                    // 거래량 정보 추가
                    if (detailMap.containsKey("tradingVolume")) {
                        dto.setTradingVolume(new BigDecimal(detailMap.get("tradingVolume").toString()));
                    }
                } else {
                    dto.setTradingVolume(BigDecimal.valueOf(score));
                    dto.setVolumeRank(rank);

                    // 거래대금 정보 추가
                    if (detailMap.containsKey("tradingValue")) {
                        dto.setTradingValue(new BigDecimal(detailMap.get("tradingValue").toString()));
                    }
                }

                // 공통 정보
                if (detailMap.containsKey("updatedAt")) {
                    dto.setUpdatedAt(Instant.parse(detailMap.get("updatedAt").toString()));
                }
                if (detailMap.containsKey("startTime")) {
                    dto.setStartTime(Instant.parse(detailMap.get("startTime").toString()));
                }
                if (detailMap.containsKey("endTime")) {
                    dto.setEndTime(Instant.parse(detailMap.get("endTime").toString()));
                }

                rankList.add(dto);
                rank++;
            }

            log.info("[RANK] {} TOP {} 조회 완료 - {} 건",
                    isValueRank ? "거래대금" : "거래량", n, rankList.size());

            return rankList;

        } catch (Exception e) {
            log.error("[RANK] Redis 랭킹 조회 실패. key: {}", key, e);
            return Collections.emptyList();
        }
    }

    /**
     * 특정 종목의 랭킹 정보 조회
     *
     * @param ticker 종목 코드
     * @return 랭킹 정보
     */
    public TradingRankDTO getRankByTicker(String ticker) {
        try {
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

            // 거래대금 랭킹 조회
            Long valueRank = zSetOps.reverseRank(RANK_VALUE_KEY, ticker);
            Double tradingValue = zSetOps.score(RANK_VALUE_KEY, ticker);

            // 거래량 랭킹 조회
            Long volumeRank = zSetOps.reverseRank(RANK_VOLUME_KEY, ticker);
            Double tradingVolume = zSetOps.score(RANK_VOLUME_KEY, ticker);

            if (valueRank == null && volumeRank == null) {
                log.warn("[RANK] 종목 {} 의 랭킹 정보를 찾을 수 없습니다.", ticker);
                return null;
            }

            // 상세 정보 조회
            String detailKey = RANK_DETAIL_KEY + ticker;
            Map<Object, Object> detailMap = redisTemplate.opsForHash().entries(detailKey);

            TradingRankDTO dto = TradingRankDTO.builder()
                    .ticker(ticker)
                    .valueRank(valueRank != null ? valueRank.intValue() + 1 : null)
                    .volumeRank(volumeRank != null ? volumeRank.intValue() + 1 : null)
                    .tradingValue(tradingValue != null ? BigDecimal.valueOf(tradingValue) : null)
                    .tradingVolume(tradingVolume != null ? BigDecimal.valueOf(tradingVolume) : null)
                    .build();

            // 공통 정보
            if (detailMap.containsKey("updatedAt")) {
                dto.setUpdatedAt(Instant.parse(detailMap.get("updatedAt").toString()));
            }
            if (detailMap.containsKey("startTime")) {
                dto.setStartTime(Instant.parse(detailMap.get("startTime").toString()));
            }
            if (detailMap.containsKey("endTime")) {
                dto.setEndTime(Instant.parse(detailMap.get("endTime").toString()));
            }

            return dto;

        } catch (Exception e) {
            log.error("[RANK] 종목 {} 의 랭킹 조회 실패", ticker, e);
            return null;
        }
    }


    /**
     * Redis에서 현재가 직접 조회 (순환 의존성 방지)
     */
    private CurrentPrice getCurrentPriceFromRedis(String ticker) {
        try {
            String redisKey = "price:" + ticker;
            Object data = redisTemplate.opsForValue().get(redisKey);

            if (data == null) {
                return null;
            }

            // LinkedHashMap -> CurrentPrice 변환
            return objectMapper.convertValue(data, CurrentPrice.class);

        } catch (Exception e) {
            log.error("[RANK] Failed to get current price from Redis: ticker={}", ticker, e);
            return null;
        }
    }

    /**
     * 거래 통계 내부 클래스
     */
    @Getter
    @Builder
    private static class TradingStats {
        private String ticker;
        private BigDecimal tradingValue;
        private BigDecimal tradingVolume;
    }
}

