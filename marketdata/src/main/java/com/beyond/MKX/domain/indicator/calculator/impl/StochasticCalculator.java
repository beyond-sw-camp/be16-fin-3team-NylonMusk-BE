package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 스토캐스틱 (Stochastic Oscillator) 계산기
 * 
 * 과매수/과매도 판단 오실레이터
 * - %K Line: (현재가 - N일 최저가) / (N일 최고가 - N일 최저가) × 100
 * - %D Line: %K의 M일 이동평균
 */
@Slf4j
@Component("STOCHASTIC")
public class StochasticCalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_K_PERIOD = 14;
    private static final int DEFAULT_D_PERIOD = 3;
    private static final int DEFAULT_SMOOTH = 3;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        int kPeriod = getIntParam(params, "kPeriod", DEFAULT_K_PERIOD);
        int dPeriod = getIntParam(params, "dPeriod", DEFAULT_D_PERIOD);
        int smooth = getIntParam(params, "smooth", DEFAULT_SMOOTH);
        
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        List<Double> rawK = new ArrayList<>();
        
        // Raw %K 계산
        for (int i = 0; i < candles.size(); i++) {
            if (i < kPeriod - 1) {
                rawK.add(Double.NaN);
                continue;
            }
            
            // K 기간 동안의 최고가와 최저가
            long highest = Long.MIN_VALUE;
            long lowest = Long.MAX_VALUE;
            
            for (int j = i - kPeriod + 1; j <= i; j++) {
                if (candles.get(j).getHigh() > highest) {
                    highest = candles.get(j).getHigh();
                }
                if (candles.get(j).getLow() < lowest) {
                    lowest = candles.get(j).getLow();
                }
            }
            
            double k = 0.0;
            if (highest != lowest) {
                k = ((candles.get(i).getClose() - lowest) / (double) (highest - lowest)) * 100.0;
            }
            
            rawK.add(k);
        }
        
        // Smooth %K (%K의 smooth 기간 이동평균)
        List<Double> smoothK = new ArrayList<>();
        for (int i = 0; i < rawK.size(); i++) {
            if (i < smooth - 1 || Double.isNaN(rawK.get(i))) {
                smoothK.add(Double.NaN);
                continue;
            }
            
            double sum = 0.0;
            int count = 0;
            for (int j = i - smooth + 1; j <= i; j++) {
                if (!Double.isNaN(rawK.get(j))) {
                    sum += rawK.get(j);
                    count++;
                }
            }
            
            smoothK.add(count > 0 ? sum / count : Double.NaN);
        }
        
        // %D (%K의 dPeriod 기간 이동평균)
        List<Double> d = new ArrayList<>();
        for (int i = 0; i < smoothK.size(); i++) {
            if (i < dPeriod - 1 || Double.isNaN(smoothK.get(i))) {
                d.add(Double.NaN);
                continue;
            }
            
            double sum = 0.0;
            int count = 0;
            for (int j = i - dPeriod + 1; j <= i; j++) {
                if (!Double.isNaN(smoothK.get(j))) {
                    sum += smoothK.get(j);
                    count++;
                }
            }
            
            d.add(count > 0 ? sum / count : Double.NaN);
        }
        
        // 결과 생성
        for (int i = 0; i < candles.size(); i++) {
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(Map.of(
                            "k", smoothK.get(i),
                            "d", d.get(i)
                    ))
                    .build());
        }
        
        log.debug("[STOCHASTIC] Calculated {} data points with kPeriod={}, dPeriod={}, smooth={}", 
                result.size(), kPeriod, dPeriod, smooth);
        return result;
    }
    
    @Override
    public Map<String, Object> getDefaultParams() {
        return Map.of(
                "kPeriod", DEFAULT_K_PERIOD,
                "dPeriod", DEFAULT_D_PERIOD,
                "smooth", DEFAULT_SMOOTH
        );
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        try {
            int kPeriod = getIntParam(params, "kPeriod", DEFAULT_K_PERIOD);
            int dPeriod = getIntParam(params, "dPeriod", DEFAULT_D_PERIOD);
            int smooth = getIntParam(params, "smooth", DEFAULT_SMOOTH);
            
            return kPeriod > 0 && dPeriod > 0 && smooth > 0 
                    && kPeriod <= 100 && dPeriod <= 100 && smooth <= 100;
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
