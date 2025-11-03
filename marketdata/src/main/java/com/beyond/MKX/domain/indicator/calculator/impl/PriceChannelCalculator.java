package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 프라이스 채널 (Price Channel / Donchian Channel) 계산기
 * 
 * 지정된 기간 동안의 최고가와 최저가로 채널 형성
 * - Upper Channel: N일 최고가
 * - Middle Channel: (최고가 + 최저가) / 2
 * - Lower Channel: N일 최저가
 */
@Slf4j
@Component("PRICE_CHANNEL")
public class PriceChannelCalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_PERIOD = 20;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        for (int i = 0; i < candles.size(); i++) {
            if (i < period - 1) {
                // ✅ 데이터가 부족한 경우 건너뛰기
                continue;
            }
            
            // 지정 기간의 최고가와 최저가 찾기
            long highest = Long.MIN_VALUE;
            long lowest = Long.MAX_VALUE;
            
            for (int j = i - period + 1; j <= i; j++) {
                if (candles.get(j).getHigh() > highest) {
                    highest = candles.get(j).getHigh();
                }
                if (candles.get(j).getLow() < lowest) {
                    lowest = candles.get(j).getLow();
                }
            }
            
            double middle = (highest + lowest) / 2.0;
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(Map.of(
                            "upper", (double) highest,
                            "middle", middle,
                            "lower", (double) lowest
                    ))
                    .build());
        }
        
        log.debug("[PRICE_CHANNEL] Calculated {} data points with period={}", result.size(), period);
        return result;
    }
    
    @Override
    public Map<String, Object> getDefaultParams() {
        return Map.of("period", DEFAULT_PERIOD);
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        try {
            int period = getIntParam(params, "period", DEFAULT_PERIOD);
            return period > 0 && period <= 500;
        } catch (Exception e) {
            return false;
        }
    }
    
    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = params.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        return defaultValue;
    }
}
