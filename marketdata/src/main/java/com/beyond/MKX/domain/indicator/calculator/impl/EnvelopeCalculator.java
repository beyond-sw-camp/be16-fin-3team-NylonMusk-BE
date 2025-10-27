package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 엔벨로프 (Envelope) 계산기
 * 
 * 이동평균선을 중심으로 일정 비율 위아래로 밴드 형성
 * - Upper Band = MA × (1 + percentage)
 * - Middle Band = MA
 * - Lower Band = MA × (1 - percentage)
 */
@Slf4j
@Component("ENVELOPE")
public class EnvelopeCalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_PERIOD = 20;
    private static final double DEFAULT_PERCENTAGE = 0.025;  // 2.5%
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        double percentage = getDoubleParam(params, "percentage", DEFAULT_PERCENTAGE);
        
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
            
            // 이동평균 계산
            double sum = 0.0;
            for (int j = i - period + 1; j <= i; j++) {
                sum += candles.get(j).getClose();
            }
            double ma = sum / period;
            
            // 엔벨로프 밴드 계산
            double upper = ma * (1 + percentage);
            double lower = ma * (1 - percentage);
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(Map.of(
                            "upper", upper,
                            "middle", ma,
                            "lower", lower
                    ))
                    .build());
        }
        
        log.debug("[ENVELOPE] Calculated {} data points with period={}, percentage={}", 
                result.size(), period, percentage);
        return result;
    }
    
    @Override
    public Map<String, Object> getDefaultParams() {
        return Map.of(
                "period", DEFAULT_PERIOD,
                "percentage", DEFAULT_PERCENTAGE
        );
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        try {
            int period = getIntParam(params, "period", DEFAULT_PERIOD);
            double percentage = getDoubleParam(params, "percentage", DEFAULT_PERCENTAGE);
            
            return period > 0 && period <= 500 && percentage > 0 && percentage <= 1.0;
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
