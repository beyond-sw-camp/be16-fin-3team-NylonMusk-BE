package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 지수 이동평균선 (EMA - Exponential Moving Average) 계산기
 * 
 * 최근 데이터에 더 높은 가중치를 부여하는 이동평균
 */
@Slf4j
@Component("EMA")
public class EMACalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_PERIOD = 12;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        double multiplier = 2.0 / (period + 1);
        
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        // 첫 EMA는 SMA로 계산
        double ema = 0.0;
        for (int i = 0; i < period && i < candles.size(); i++) {
            ema += candles.get(i).getClose();
        }
        ema /= Math.min(period, candles.size());
        
        for (int i = 0; i < candles.size(); i++) {
            if (i < period - 1) {
                // ✅ 데이터가 부족한 경우 건너뛰기
                continue;
            } else if (i == period - 1) {
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime())
                        .values(Map.of("ema", ema))
                        .build());
            } else {
                // EMA = (현재가 × multiplier) + (전일 EMA × (1 - multiplier))
                ema = (candles.get(i).getClose() * multiplier) + (ema * (1 - multiplier));
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime())
                        .values(Map.of("ema", ema))
                        .build());
            }
        }
        
        log.debug("[EMA] Calculated {} data points with period={}", result.size(), period);
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
