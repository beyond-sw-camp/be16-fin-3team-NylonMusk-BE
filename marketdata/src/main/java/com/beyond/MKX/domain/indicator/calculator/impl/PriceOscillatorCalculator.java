package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Price Oscillator (PPO - Percentage Price Oscillator) 계산기
 * 
 * MACD와 유사하지만 백분율로 표시
 * 서로 다른 가격대의 주식을 비교할 때 유용
 * 
 * 계산 공식:
 * PPO = [(EMA(12) - EMA(26)) / EMA(26)] × 100
 * Signal = EMA(9, PPO)
 * Histogram = PPO - Signal
 * 
 * 해석:
 * - PPO > 0: 단기 상승 추세
 * - PPO < 0: 단기 하락 추세
 * - Signal Line 교차: 매매 신호
 */
@Slf4j
@Component("PRICE_OSCILLATOR")
public class PriceOscillatorCalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_SHORT_PERIOD = 12;
    private static final int DEFAULT_LONG_PERIOD = 26;
    private static final int DEFAULT_SIGNAL_PERIOD = 9;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        int shortPeriod = getIntParam(params, "shortPeriod", DEFAULT_SHORT_PERIOD);
        int longPeriod = getIntParam(params, "longPeriod", DEFAULT_LONG_PERIOD);
        int signalPeriod = getIntParam(params, "signalPeriod", DEFAULT_SIGNAL_PERIOD);
        
        // 1. 종가 리스트 생성
        List<Double> closePrices = new ArrayList<>();
        for (Candle candle : candles) {
            closePrices.add((double) candle.getClose());
        }
        
        // 2. 단기/장기 EMA 계산
        List<Double> shortEMA = calculateEMA(closePrices, shortPeriod);
        List<Double> longEMA = calculateEMA(closePrices, longPeriod);
        
        // 3. PPO 계산
        List<Double> ppoValues = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            if (Double.isNaN(shortEMA.get(i)) || Double.isNaN(longEMA.get(i)) || longEMA.get(i) == 0) {
                ppoValues.add(Double.NaN);
            } else {
                double ppo = ((shortEMA.get(i) - longEMA.get(i)) / longEMA.get(i)) * 100.0;
                ppoValues.add(ppo);
            }
        }
        
        // 4. Signal Line 계산 (PPO의 EMA)
        List<Double> signalLine = calculateEMA(ppoValues, signalPeriod);
        
        // 5. 결과 생성
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        for (int i = 0; i < candles.size(); i++) {
            // ✅ 유효한 값이 있는 경우에만 추가
            if (Double.isNaN(ppoValues.get(i)) || Double.isNaN(signalLine.get(i))) {
                continue;
            }
            
            double ppo = ppoValues.get(i);
            double signal = signalLine.get(i);
            double histogram = ppo - signal;
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(Map.of(
                            "ppo", ppo,
                            "signal", signal,
                            "histogram", histogram
                    ))
                    .build());
        }
        
        log.debug("[PRICE_OSCILLATOR] Calculated {} data points with periods: {}/{}/{}", 
                result.size(), shortPeriod, longPeriod, signalPeriod);
        return result;
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
        
        // 초기 SMA 계산 (NaN 제외)
        double sum = 0.0;
        int count = 0;
        
        for (int i = 0; i < Math.min(period, values.size()); i++) {
            if (!Double.isNaN(values.get(i))) {
                sum += values.get(i);
                count++;
            }
        }
        
        if (count == 0) {
            return Collections.nCopies(values.size(), Double.NaN);
        }
        
        double currentEMA = sum / count;
        
        // EMA 계산
        for (int i = 0; i < values.size(); i++) {
            if (i < period - 1) {
                ema.add(Double.NaN);
            } else if (i == period - 1) {
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
                "shortPeriod", DEFAULT_SHORT_PERIOD,
                "longPeriod", DEFAULT_LONG_PERIOD,
                "signalPeriod", DEFAULT_SIGNAL_PERIOD
        );
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        try {
            int shortPeriod = getIntParam(params, "shortPeriod", DEFAULT_SHORT_PERIOD);
            int longPeriod = getIntParam(params, "longPeriod", DEFAULT_LONG_PERIOD);
            int signalPeriod = getIntParam(params, "signalPeriod", DEFAULT_SIGNAL_PERIOD);
            
            return shortPeriod > 0 && longPeriod > 0 && signalPeriod > 0 
                    && shortPeriod < longPeriod;
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
