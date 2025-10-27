package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MACD (Moving Average Convergence Divergence) 계산기
 * 
 * 추세 추종 모멘텀 지표
 * MACD Line, Signal Line, Histogram 계산
 */
@Slf4j
@Component("MACD")
public class MACDCalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_FAST_PERIOD = 12;
    private static final int DEFAULT_SLOW_PERIOD = 26;
    private static final int DEFAULT_SIGNAL_PERIOD = 9;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        int fastPeriod = getIntParam(params, "fastPeriod", DEFAULT_FAST_PERIOD);
        int slowPeriod = getIntParam(params, "slowPeriod", DEFAULT_SLOW_PERIOD);
        int signalPeriod = getIntParam(params, "signalPeriod", DEFAULT_SIGNAL_PERIOD);
        
        // Fast EMA 계산
        List<Double> fastEMA = calculateEMA(candles, fastPeriod);
        
        // Slow EMA 계산
        List<Double> slowEMA = calculateEMA(candles, slowPeriod);
        
        // MACD Line = Fast EMA - Slow EMA
        List<Double> macdLine = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            if (Double.isNaN(fastEMA.get(i)) || Double.isNaN(slowEMA.get(i))) {
                macdLine.add(Double.NaN);
            } else {
                macdLine.add(fastEMA.get(i) - slowEMA.get(i));
            }
        }
        
        // Signal Line = MACD Line의 EMA
        List<Double> signalLine = calculateEMAFromValues(macdLine, signalPeriod);
        
        // Histogram = MACD Line - Signal Line
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            Map<String, Double> values = new HashMap<>();
            
            if (!Double.isNaN(macdLine.get(i))) {
                values.put("macd", macdLine.get(i));
            } else {
                values.put("macd", Double.NaN);
            }
            
            if (!Double.isNaN(signalLine.get(i))) {
                values.put("signal", signalLine.get(i));
                values.put("histogram", macdLine.get(i) - signalLine.get(i));
            } else {
                values.put("signal", Double.NaN);
                values.put("histogram", Double.NaN);
            }
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(values)
                    .build());
        }
        
        log.debug("[MACD] Calculated {} data points with fast={}, slow={}, signal={}", 
                result.size(), fastPeriod, slowPeriod, signalPeriod);
        return result;
    }
    
    private List<Double> calculateEMA(List<Candle> candles, int period) {
        List<Double> ema = new ArrayList<>();
        double multiplier = 2.0 / (period + 1);
        
        // 첫 EMA는 SMA
        double currentEMA = 0.0;
        for (int i = 0; i < period && i < candles.size(); i++) {
            currentEMA += candles.get(i).getClose();
        }
        currentEMA /= Math.min(period, candles.size());
        
        for (int i = 0; i < candles.size(); i++) {
            if (i < period - 1) {
                ema.add(Double.NaN);
            } else if (i == period - 1) {
                ema.add(currentEMA);
            } else {
                currentEMA = (candles.get(i).getClose() * multiplier) + (currentEMA * (1 - multiplier));
                ema.add(currentEMA);
            }
        }
        
        return ema;
    }
    
    private List<Double> calculateEMAFromValues(List<Double> values, int period) {
        List<Double> ema = new ArrayList<>();
        double multiplier = 2.0 / (period + 1);
        
        // 첫 유효한 값 찾기
        int firstValidIndex = -1;
        for (int i = 0; i < values.size(); i++) {
            if (!Double.isNaN(values.get(i))) {
                firstValidIndex = i;
                break;
            }
        }
        
        if (firstValidIndex == -1) {
            for (int i = 0; i < values.size(); i++) {
                ema.add(Double.NaN);
            }
            return ema;
        }
        
        // 첫 EMA는 SMA
        double currentEMA = 0.0;
        int count = 0;
        for (int i = firstValidIndex; i < firstValidIndex + period && i < values.size(); i++) {
            if (!Double.isNaN(values.get(i))) {
                currentEMA += values.get(i);
                count++;
            }
        }
        currentEMA /= count;
        
        for (int i = 0; i < values.size(); i++) {
            if (i < firstValidIndex + period - 1) {
                ema.add(Double.NaN);
            } else if (i == firstValidIndex + period - 1) {
                ema.add(currentEMA);
            } else {
                if (!Double.isNaN(values.get(i))) {
                    currentEMA = (values.get(i) * multiplier) + (currentEMA * (1 - multiplier));
                    ema.add(currentEMA);
                } else {
                    ema.add(Double.NaN);
                }
            }
        }
        
        return ema;
    }
    
    @Override
    public Map<String, Object> getDefaultParams() {
        return Map.of(
                "fastPeriod", DEFAULT_FAST_PERIOD,
                "slowPeriod", DEFAULT_SLOW_PERIOD,
                "signalPeriod", DEFAULT_SIGNAL_PERIOD
        );
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        if (params == null) {
            return false;
        }
        
        try {
            int fastPeriod = getIntParam(params, "fastPeriod", DEFAULT_FAST_PERIOD);
            int slowPeriod = getIntParam(params, "slowPeriod", DEFAULT_SLOW_PERIOD);
            int signalPeriod = getIntParam(params, "signalPeriod", DEFAULT_SIGNAL_PERIOD);
            
            return fastPeriod > 0 && slowPeriod > 0 && signalPeriod > 0 
                    && fastPeriod < slowPeriod;
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
