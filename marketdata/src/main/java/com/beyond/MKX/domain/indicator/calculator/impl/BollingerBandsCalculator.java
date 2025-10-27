package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 볼린저 밴드 (Bollinger Bands) 계산기
 * 
 * 상단밴드, 중간밴드(MA), 하단밴드 계산
 */
@Slf4j
@Component("BOLLINGER_BANDS")
public class BollingerBandsCalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_PERIOD = 20;
    private static final double DEFAULT_STD_DEV = 2.0;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        double stdDevMultiplier = getDoubleParam(params, "stdDev", DEFAULT_STD_DEV);
        
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        for (int i = 0; i < candles.size(); i++) {
            if (i < period - 1) {
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime())
                        .values(Map.of(
                                "upper", Double.NaN,
                                "middle", Double.NaN,
                                "lower", Double.NaN
                        ))
                        .build());
                continue;
            }
            
            // 중간밴드 (SMA)
            double sum = 0.0;
            for (int j = i - period + 1; j <= i; j++) {
                sum += candles.get(j).getClose();
            }
            double sma = sum / period;
            
            // 표준편차 계산
            double variance = 0.0;
            for (int j = i - period + 1; j <= i; j++) {
                double diff = candles.get(j).getClose() - sma;
                variance += diff * diff;
            }
            double stdDev = Math.sqrt(variance / period);
            
            // 상단밴드 = 중간밴드 + (표준편차 × 배수)
            // 하단밴드 = 중간밴드 - (표준편차 × 배수)
            double upper = sma + (stdDev * stdDevMultiplier);
            double lower = sma - (stdDev * stdDevMultiplier);
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(Map.of(
                            "upper", upper,
                            "middle", sma,
                            "lower", lower
                    ))
                    .build());
        }
        
        log.debug("[BOLLINGER_BANDS] Calculated {} data points with period={}, stdDev={}", 
                result.size(), period, stdDevMultiplier);
        return result;
    }
    
    @Override
    public Map<String, Object> getDefaultParams() {
        return Map.of(
                "period", DEFAULT_PERIOD,
                "stdDev", DEFAULT_STD_DEV
        );
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        if (params == null) {
            return false;
        }
        
        try {
            int period = getIntParam(params, "period", DEFAULT_PERIOD);
            double stdDev = getDoubleParam(params, "stdDev", DEFAULT_STD_DEV);
            
            return period > 0 && period <= 500 && stdDev > 0 && stdDev <= 5;
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
    
    private double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = params.get(key);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        return defaultValue;
    }
}
