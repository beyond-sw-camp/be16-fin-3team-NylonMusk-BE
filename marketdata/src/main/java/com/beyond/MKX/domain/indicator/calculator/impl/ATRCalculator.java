package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * ATR (Average True Range) 계산기
 * 
 * 변동성 측정 지표
 * - True Range = max(고가-저가, |고가-전일종가|, |저가-전일종가|)
 * - ATR = True Range의 N일 이동평균
 */
@Slf4j
@Component("ATR")
public class ATRCalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_PERIOD = 14;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.size() < 2) {
            return Collections.emptyList();
        }
        
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        List<Double> trueRanges = new ArrayList<>();
        
        // 첫 번째 캔들
        result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(candles.get(0).getTime())
                .values(Map.of("atr", Double.NaN, "tr", Double.NaN))
                .build());
        trueRanges.add(Double.NaN);
        
        // True Range 계산
        for (int i = 1; i < candles.size(); i++) {
            Candle current = candles.get(i);
            Candle previous = candles.get(i - 1);
            
            double highLow = current.getHigh() - current.getLow();
            double highClose = Math.abs(current.getHigh() - previous.getClose());
            double lowClose = Math.abs(current.getLow() - previous.getClose());
            
            double tr = Math.max(highLow, Math.max(highClose, lowClose));
            trueRanges.add(tr);
        }
        
        // ATR 계산 (Wilder's Smoothing)
        double atr = 0.0;
        
        // 첫 ATR은 단순 평균
        for (int i = 1; i <= period && i < trueRanges.size(); i++) {
            if (!Double.isNaN(trueRanges.get(i))) {
                atr += trueRanges.get(i);
            }
        }
        atr /= period;
        
        for (int i = 1; i < candles.size(); i++) {
            if (i < period) {
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime())
                        .values(Map.of("atr", Double.NaN, "tr", trueRanges.get(i)))
                        .build());
            } else if (i == period) {
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime())
                        .values(Map.of("atr", atr, "tr", trueRanges.get(i)))
                        .build());
            } else {
                // Wilder's Smoothing: ATR = (Previous ATR × (period - 1) + Current TR) / period
                atr = ((atr * (period - 1)) + trueRanges.get(i)) / period;
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime())
                        .values(Map.of("atr", atr, "tr", trueRanges.get(i)))
                        .build());
            }
        }
        
        log.debug("[ATR] Calculated {} data points with period={}", result.size(), period);
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
            return period > 0 && period <= 100;
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
