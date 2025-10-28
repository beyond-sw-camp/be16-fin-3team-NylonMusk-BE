package com.beyond.MKX.domain.indicator.service;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.chart.repository.CandleInfluxRepository;
import com.beyond.MKX.domain.chart.service.ChartService;
import com.beyond.MKX.domain.indicator.cache.IndicatorCacheManager;
import com.beyond.MKX.domain.indicator.cache.IncrementalCalculationManager;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorRequestDTO;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import com.beyond.MKX.domain.indicator.dto.UserIndicatorConfigDTO;
import com.beyond.MKX.domain.indicator.enums.IndicatorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 보조지표 서비스
 * 
 * 보조지표 계산 및 사용자 설정 관리
 * - on-demand 계산 방식
 * - 사용자별 지표 설정 Redis 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorService {

    private final ChartService chartService;
    private final CandleInfluxRepository candleInfluxRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;
    private final IndicatorCacheManager cacheManager;
    private final IncrementalCalculationManager incrementalManager;

    /**
     * 보조지표 계산
     * 
     * @param request 지표 계산 요청
     * @param start 시작 시각
     * @param end 종료 시각
     * @return 계산된 지표 결과
     */
    public IndicatorResultDTO calculateIndicator(IndicatorRequestDTO request, Instant start, Instant end) {
        try {
            log.info("[INDICATOR] Calculating indicator: type={}, ticker={}, interval={}", 
                    request.getIndicatorType(), request.getTicker(), request.getInterval());
            
            // 0. 캐시 확인
            Map<String, Object> params = request.getParams() != null ? request.getParams() : new HashMap<>();
            IndicatorResultDTO cachedResult = cacheManager.getFromCache(
                    request.getTicker(), 
                    request.getInterval(), 
                    request.getIndicatorType(), 
                    params
            );
            
            if (cachedResult != null) {
                log.info("[INDICATOR] ⚡ Using cached result: type={}, dataPoints={}", 
                        request.getIndicatorType(), cachedResult.getDataPointCount());
                return cachedResult;
            }
            
            // 1. 캔들 데이터 조회
            List<Candle> candles = chartService.getCandles(
                    request.getTicker(), 
                    request.getInterval(), 
                    start, 
                    end
            );
            
            if (candles.isEmpty()) {
                log.warn("[INDICATOR] No candles found for calculation");
                return createEmptyResult(request);
            }
            
            // 2. 지표 계산기 가져오기
            IndicatorCalculator calculator = getCalculator(request.getIndicatorType());
            
            // 3. 파라미터 검증
            if (params.isEmpty()) {
                params = calculator.getDefaultParams();
            }
            
            if (!calculator.validateParams(params)) {
                log.warn("[INDICATOR] Invalid params for {}: {}", request.getIndicatorType(), params);
                params = calculator.getDefaultParams();
            }
            
            // 4. 지표 계산
            List<IndicatorResultDTO.IndicatorDataPoint> dataPoints = calculator.calculate(candles, params);
            
            // 5. 결과 생성
            IndicatorResultDTO result = IndicatorResultDTO.builder()
                    .ticker(request.getTicker())
                    .interval(request.getInterval())
                    .indicatorType(request.getIndicatorType())
                    .params(params)
                    .data(dataPoints)
                    .calculatedAt(Instant.now())
                    .dataPointCount(dataPoints.size())
                    .build();
            
            // 6. 캐시 저장
            boolean isConfirmed = isDataConfirmed(end);
            cacheManager.saveToCache(result, isConfirmed);
            
            // 7. 증분 계산 상태 저장
            if (incrementalManager.canUseIncremental(request.getIndicatorType())) {
                incrementalManager.saveCalculationState(
                        request.getTicker(), 
                        request.getInterval(), 
                        request.getIndicatorType(), 
                        params, 
                        result
                );
            }
            
            log.info("[INDICATOR] ✅ Successfully calculated {} with {} data points (cached: {})", 
                    request.getIndicatorType(), dataPoints.size(), isConfirmed);
            
            return result;
            
        } catch (Exception e) {
            log.error("[INDICATOR] Failed to calculate indicator: {}", request, e);
            throw new RuntimeException("Failed to calculate indicator", e);
        }
    }

    /**
     * 여러 지표를 한 번에 계산
     */
    public List<IndicatorResultDTO> calculateMultipleIndicators(
            String ticker, String interval, List<IndicatorRequestDTO> requests, Instant start, Instant end) {
        
        log.info("[INDICATOR] Calculating {} indicators for ticker={}, interval={}", 
                requests.size(), ticker, interval);
        
        List<IndicatorResultDTO> results = new ArrayList<>();
        
        for (IndicatorRequestDTO request : requests) {
            try {
                IndicatorResultDTO result = calculateIndicator(request, start, end);
                results.add(result);
            } catch (Exception e) {
                log.error("[INDICATOR] Failed to calculate {}", request.getIndicatorType(), e);
            }
        }
        
        return results;
    }

    /**
     * 사용자 지표 설정 저장
     */
    public void saveUserIndicatorConfig(UserIndicatorConfigDTO config) {
        try {
            String redisKey = config.toRedisKey();
            redisTemplate.opsForValue().set(redisKey, config, 30, TimeUnit.DAYS);
            
            log.info("[INDICATOR] Saved user config: userId={}, ticker={}, indicator={}, enabled={}", 
                    config.getUserId(), config.getTicker(), config.getIndicatorType(), config.isEnabled());
            
        } catch (Exception e) {
            log.error("[INDICATOR] Failed to save user config: {}", config, e);
            throw new RuntimeException("Failed to save user indicator config", e);
        }
    }

    /**
     * 사용자 지표 설정 조회
     */
    public UserIndicatorConfigDTO getUserIndicatorConfig(String userId, String ticker, 
                                                          String interval, IndicatorType indicatorType) {
        try {
            String redisKey = String.format("indicator:config:%s:%s:%s:%s", 
                    userId, ticker, interval, indicatorType.name());
            
            Object data = redisTemplate.opsForValue().get(redisKey);
            
            if (data == null) {
                return null;
            }
            
            return objectMapper.convertValue(data, UserIndicatorConfigDTO.class);
            
        } catch (Exception e) {
            log.error("[INDICATOR] Failed to get user config", e);
            return null;
        }
    }

    /**
     * 사용자의 활성화된 지표 목록 조회
     */
    public List<UserIndicatorConfigDTO> getEnabledIndicators(String userId, String ticker, String interval) {
        try {
            String pattern = String.format("indicator:config:%s:%s:%s:*", userId, ticker, interval);
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys == null || keys.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<UserIndicatorConfigDTO> configs = new ArrayList<>();
            
            for (String key : keys) {
                Object data = redisTemplate.opsForValue().get(key);
                if (data != null) {
                    UserIndicatorConfigDTO config = objectMapper.convertValue(data, UserIndicatorConfigDTO.class);
                    if (config.isEnabled()) {
                        configs.add(config);
                    }
                }
            }
            
            log.info("[INDICATOR] Found {} enabled indicators for userId={}, ticker={}, interval={}", 
                    configs.size(), userId, ticker, interval);
            
            return configs;
            
        } catch (Exception e) {
            log.error("[INDICATOR] Failed to get enabled indicators", e);
            return Collections.emptyList();
        }
    }

    /**
     * 사용자 지표 설정 삭제
     */
    public void deleteUserIndicatorConfig(String userId, String ticker, String interval, IndicatorType indicatorType) {
        try {
            String redisKey = String.format("indicator:config:%s:%s:%s:%s", 
                    userId, ticker, interval, indicatorType.name());
            
            redisTemplate.delete(redisKey);
            
            log.info("[INDICATOR] Deleted user config: userId={}, ticker={}, indicator={}", 
                    userId, ticker, indicatorType);
            
        } catch (Exception e) {
            log.error("[INDICATOR] Failed to delete user config", e);
            throw new RuntimeException("Failed to delete user indicator config", e);
        }
    }

    /**
     * 지표 타입에 맞는 계산기 가져오기
     */
    private IndicatorCalculator getCalculator(IndicatorType indicatorType) {
        try {
            return applicationContext.getBean(indicatorType.name(), IndicatorCalculator.class);
        } catch (Exception e) {
            log.error("[INDICATOR] Calculator not found for type: {}", indicatorType, e);
            throw new RuntimeException("Indicator calculator not found: " + indicatorType);
        }
    }

    /**
     * 빈 결과 생성
     */
    private IndicatorResultDTO createEmptyResult(IndicatorRequestDTO request) {
        return IndicatorResultDTO.builder()
                .ticker(request.getTicker())
                .interval(request.getInterval())
                .indicatorType(request.getIndicatorType())
                .params(request.getParams())
                .data(Collections.emptyList())
                .calculatedAt(Instant.now())
                .dataPointCount(0)
                .build();
    }

    /**
     * 데이터 확정 여부 판단
     * 
     * 현재 시간 이후의 데이터는 미확정 (가변)
     */
    private boolean isDataConfirmed(Instant end) {
        return end.isBefore(Instant.now().minusSeconds(60)); // 1분 이상 경과한 데이터는 확정
    }

    /**
     * 캐시 무효화 (API 용)
     */
    public void invalidateCache(String ticker, String interval, IndicatorType indicatorType, Map<String, Object> params) {
        cacheManager.invalidateCache(ticker, interval, indicatorType, params);
    }

    /**
     * 종목의 모든 캐시 무효화
     */
    public void invalidateAllCacheForTicker(String ticker) {
        cacheManager.invalidateAllCacheForTicker(ticker);
        incrementalManager.invalidateAllStatesForTicker(ticker);
    }

    /**
     * 캐시 통계 조회
     */
    public Map<String, Object> getCacheStats(String ticker) {
        return cacheManager.getCacheStats(ticker);
    }
}
