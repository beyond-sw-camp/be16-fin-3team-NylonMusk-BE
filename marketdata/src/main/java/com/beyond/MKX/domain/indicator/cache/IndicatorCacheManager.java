package com.beyond.MKX.domain.indicator.cache;

import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import com.beyond.MKX.domain.indicator.enums.IndicatorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 보조지표 캐시 매니저
 * 
 * 계산된 지표 결과를 Redis에 캐싱하여 성능 향상
 * - 최근 계산 결과 5분간 캐싱
 * - 확정된 캔들의 지표는 장기 캐싱 (24시간)
 * - 캐시 키 전략: indicator:cache:{ticker}:{interval}:{indicatorType}:{params}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final int SHORT_CACHE_TTL_MINUTES = 5;      // 단기 캐시: 5분
    private static final int LONG_CACHE_TTL_HOURS = 24;        // 장기 캐시: 24시간
    private static final String CACHE_KEY_PREFIX = "indicator:cache:";

    /**
     * 지표 결과 캐시 조회
     * 
     * @param ticker 종목코드
     * @param interval 캔들 간격
     * @param indicatorType 지표 타입
     * @param params 지표 파라미터
     * @return 캐시된 지표 결과 (없으면 null)
     */
    public IndicatorResultDTO getFromCache(String ticker, String interval, 
                                           IndicatorType indicatorType, Map<String, Object> params) {
        try {
            String cacheKey = generateCacheKey(ticker, interval, indicatorType, params);
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                IndicatorResultDTO result = objectMapper.convertValue(cached, IndicatorResultDTO.class);
                
                // 캐시 유효성 검증
                if (isCacheValid(result)) {
                    log.debug("[INDICATOR-CACHE] 🎯 Cache HIT: {}", cacheKey);
                    return result;
                } else {
                    // 유효하지 않은 캐시 삭제
                    redisTemplate.delete(cacheKey);
                    log.debug("[INDICATOR-CACHE] ⚠️ Cache expired and deleted: {}", cacheKey);
                }
            }
            
            log.debug("[INDICATOR-CACHE] ❌ Cache MISS: {}", cacheKey);
            return null;
            
        } catch (Exception e) {
            log.error("[INDICATOR-CACHE] Failed to get from cache", e);
            return null;
        }
    }

    /**
     * 지표 결과 캐시 저장
     * 
     * @param result 지표 계산 결과
     * @param isConfirmed 확정된 데이터 여부
     */
    public void saveToCache(IndicatorResultDTO result, boolean isConfirmed) {
        try {
            String cacheKey = generateCacheKey(
                    result.getTicker(), 
                    result.getInterval(), 
                    result.getIndicatorType(), 
                    result.getParams()
            );
            
            // 확정된 데이터는 장기 캐싱, 미확정 데이터는 단기 캐싱
            int ttl = isConfirmed ? LONG_CACHE_TTL_HOURS * 60 : SHORT_CACHE_TTL_MINUTES;
            
            redisTemplate.opsForValue().set(cacheKey, result, ttl, TimeUnit.MINUTES);
            
            log.debug("[INDICATOR-CACHE] 💾 Cached: {} (TTL: {}min, confirmed: {})", 
                    cacheKey, ttl, isConfirmed);
            
        } catch (Exception e) {
            log.error("[INDICATOR-CACHE] Failed to save to cache", e);
        }
    }

    /**
     * 특정 지표 캐시 삭제
     */
    public void invalidateCache(String ticker, String interval, 
                                IndicatorType indicatorType, Map<String, Object> params) {
        try {
            String cacheKey = generateCacheKey(ticker, interval, indicatorType, params);
            redisTemplate.delete(cacheKey);
            log.debug("[INDICATOR-CACHE] 🗑️ Cache invalidated: {}", cacheKey);
        } catch (Exception e) {
            log.error("[INDICATOR-CACHE] Failed to invalidate cache", e);
        }
    }

    /**
     * 종목의 모든 지표 캐시 삭제
     */
    public void invalidateAllCacheForTicker(String ticker) {
        try {
            String pattern = CACHE_KEY_PREFIX + ticker + ":*";
            var keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("[INDICATOR-CACHE] 🗑️ Invalidated {} caches for ticker: {}", 
                        keys.size(), ticker);
            }
        } catch (Exception e) {
            log.error("[INDICATOR-CACHE] Failed to invalidate all cache for ticker", e);
        }
    }

    /**
     * 캐시 키 생성
     */
    private String generateCacheKey(String ticker, String interval, 
                                    IndicatorType indicatorType, Map<String, Object> params) {
        StringBuilder keyBuilder = new StringBuilder(CACHE_KEY_PREFIX);
        keyBuilder.append(ticker).append(":");
        keyBuilder.append(interval).append(":");
        keyBuilder.append(indicatorType.name());
        
        // 파라미터를 정렬하여 일관된 키 생성
        if (params != null && !params.isEmpty()) {
            keyBuilder.append(":");
            params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> keyBuilder.append(entry.getKey())
                            .append("=")
                            .append(entry.getValue())
                            .append(","));
            
            // 마지막 쉼표 제거
            keyBuilder.setLength(keyBuilder.length() - 1);
        }
        
        return keyBuilder.toString();
    }

    /**
     * 캐시 유효성 검증
     * 
     * 5분 이상 경과한 캐시는 무효화
     */
    private boolean isCacheValid(IndicatorResultDTO result) {
        if (result == null || result.getCalculatedAt() == null) {
            return false;
        }
        
        Duration timeSinceCalculation = Duration.between(
                result.getCalculatedAt(), 
                Instant.now()
        );
        
        // 5분 이상 경과하면 무효
        return timeSinceCalculation.toMinutes() < SHORT_CACHE_TTL_MINUTES;
    }

    /**
     * 캐시 통계 조회
     */
    public Map<String, Object> getCacheStats(String ticker) {
        try {
            String pattern = CACHE_KEY_PREFIX + ticker + ":*";
            var keys = redisTemplate.keys(pattern);
            int cacheCount = keys != null ? keys.size() : 0;
            
            return Map.of(
                    "ticker", ticker,
                    "cachedIndicators", cacheCount,
                    "cacheKeyPrefix", CACHE_KEY_PREFIX + ticker
            );
        } catch (Exception e) {
            log.error("[INDICATOR-CACHE] Failed to get cache stats", e);
            return Map.of("error", e.getMessage());
        }
    }
}
