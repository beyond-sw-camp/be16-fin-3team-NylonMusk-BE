package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * RSI (Relative Strength Index) 계산기
 * 
 * 과매수/과매도 판단 지표
 */
@Slf4j
@Component("RSI")
public class RSICalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_PERIOD = 14;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.size() < 2) {
            return Collections.emptyList();
        }
        
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        // 가격 변화 계산
        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();
        
        for (int i = 1; i < candles.size(); i++) {
            double change = candles.get(i).getClose() - candles.get(i - 1).getClose();
            gains.add(change > 0 ? change : 0);
            losses.add(change < 0 ? -change : 0);
        }
        
        // 첫 번째 평균 이득/손실 계산
        double avgGain = 0.0;
        double avgLoss = 0.0;
        
        for (int i = 0; i < period && i < gains.size(); i++) {
            avgGain += gains.get(i);
            avgLoss += losses.get(i);
        }
        avgGain /= period;
        avgLoss /= period;
        
        // ✅ 데이터가 부족한 경우는 이후 필터링에서 제거되므로 건너뛰기
        
        // RSI 계산
        for (int i = 0; i < gains.size(); i++) {
            if (i < period - 1) {
                // ✅ 계산 불가능한 경우 건너뛰기
                continue;
            }
            
            if (i == period - 1) {
                // 첫 RSI
                double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
                double rsi = 100 - (100 / (1 + rs));
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i + 1).getTime())
                        .values(Map.of("rsi", rsi))
                        .build());
            } else {
                // Smoothed RSI
                avgGain = (avgGain * (period - 1) + gains.get(i)) / period;
                avgLoss = (avgLoss * (period - 1) + losses.get(i)) / period;
                
                double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
                double rsi = 100 - (100 / (1 + rs));
                
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i + 1).getTime())
                        .values(Map.of("rsi", rsi))
                        .build());
            }
        }
        
        log.debug("[RSI] Calculated {} data points with period={}", result.size(), period);
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
