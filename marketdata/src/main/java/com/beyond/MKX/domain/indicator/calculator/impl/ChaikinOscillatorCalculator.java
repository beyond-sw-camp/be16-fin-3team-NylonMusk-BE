package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Chaikin Oscillator 계산기
 * 
 * AD Line의 3일 EMA와 10일 EMA의 차이
 * 매수/매도 압력의 변화를 감지
 * 
 * 계산 공식:
 * 1. AD Line = 누적 [(종가-저가) - (고가-종가)] / (고가-저가) * 거래량
 * 2. Chaikin Oscillator = EMA(3, AD Line) - EMA(10, AD Line)
 */
@Slf4j
@Component("CHAIKIN_OSCILLATOR")
public class ChaikinOscillatorCalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_SHORT_PERIOD = 3;
    private static final int DEFAULT_LONG_PERIOD = 10;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        int shortPeriod = getIntParam(params, "shortPeriod", DEFAULT_SHORT_PERIOD);
        int longPeriod = getIntParam(params, "longPeriod", DEFAULT_LONG_PERIOD);
        
        // 1. AD Line 계산
        List<Double> adLine = calculateADLine(candles);
        
        // 2. AD Line의 EMA 계산
        List<Double> shortEMA = calculateEMA(adLine, shortPeriod);
        List<Double> longEMA = calculateEMA(adLine, longPeriod);
        
        // 3. Chaikin Oscillator 계산
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        for (int i = 0; i < candles.size(); i++) {
            // ✅ 두 EMA가 모두 유효한 경우에만 계산
            if (Double.isNaN(shortEMA.get(i)) || Double.isNaN(longEMA.get(i))) {
                continue;
            }
            
            double chaikin = shortEMA.get(i) - longEMA.get(i);
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(Map.of("chaikin", chaikin))
                    .build());
        }
        
        log.debug("[CHAIKIN_OSCILLATOR] Calculated {} data points", result.size());
        return result;
    }
    
    /**
     * Accumulation/Distribution Line 계산
     */
    private List<Double> calculateADLine(List<Candle> candles) {
        List<Double> adLine = new ArrayList<>();
        double cumulative = 0.0;
        
        for (Candle candle : candles) {
            double high = candle.getHigh();
            double low = candle.getLow();
            double close = candle.getClose();
            double volume = candle.getVolume() != null ? candle.getVolume().doubleValue() : 0.0;
            
            // Money Flow Multiplier 계산
            double mfm = 0.0;
            if (high != low) {
                mfm = ((close - low) - (high - close)) / (high - low);
            }
            
            // Money Flow Volume
            double mfv = mfm * volume;
            
            // 누적
            cumulative += mfv;
            adLine.add(cumulative);
        }
        
        return adLine;
    }
    
    /**
     * EMA 계산
     */
    private List<Double> calculateEMA(List<Double> values, int period) {
        List<Double> ema = new ArrayList<>();
        
        if (values.isEmpty()) {
            return ema;
        }
        
        double multiplier = 2.0 / (period + 1);
        
        // 초기 SMA 계산
        double sum = 0.0;
        for (int i = 0; i < Math.min(period, values.size()); i++) {
            sum += values.get(i);
        }
        double currentEMA = sum / Math.min(period, values.size());
        
        // EMA 계산
        for (int i = 0; i < values.size(); i++) {
            if (i < period - 1) {
                ema.add(Double.NaN);
            } else if (i == period - 1) {
                ema.add(currentEMA);
            } else {
                currentEMA = (values.get(i) * multiplier) + (currentEMA * (1 - multiplier));
                ema.add(currentEMA);
            }
        }
        
        return ema;
    }
    
    @Override
    public Map<String, Object> getDefaultParams() {
        return Map.of(
                "shortPeriod", DEFAULT_SHORT_PERIOD,
                "longPeriod", DEFAULT_LONG_PERIOD
        );
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        try {
            int shortPeriod = getIntParam(params, "shortPeriod", DEFAULT_SHORT_PERIOD);
            int longPeriod = getIntParam(params, "longPeriod", DEFAULT_LONG_PERIOD);
            
            return shortPeriod > 0 && longPeriod > 0 && shortPeriod < longPeriod;
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
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        return defaultValue;
    }
}
