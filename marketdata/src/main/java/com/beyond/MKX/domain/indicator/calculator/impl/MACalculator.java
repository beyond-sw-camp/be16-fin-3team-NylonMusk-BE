package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 이동평균선 (MA - Moving Average) 계산기
 * 
 * 단순 이동평균선 계산
 */
@Slf4j
@Component("MA")
public class MACalculator implements IndicatorCalculator {
    
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
                // 데이터가 부족한 경우 null
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime())
                        .values(Map.of("ma", Double.NaN))
                        .build());
                continue;
            }
            
            // period 개수만큼의 종가 평균 계산
            double sum = 0.0;
            for (int j = i - period + 1; j <= i; j++) {
                sum += candles.get(j).getClose();
            }
            double ma = sum / period;
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(Map.of("ma", ma))
                    .build());
        }
        
        log.debug("[MA] Calculated {} data points with period={}", result.size(), period);
        return result;
    }
    
    @Override
    public Map<String, Object> getDefaultParams() {
        return Map.of("period", DEFAULT_PERIOD);
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        if (params == null || !params.containsKey("period")) {
            return false;
        }
        
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
