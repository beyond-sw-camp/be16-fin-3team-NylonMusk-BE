package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Volume Oscillator 계산기
 * 
 * 거래량의 단기/장기 이동평균 차이를 백분율로 표시
 * 거래량 추세의 변화를 감지
 * 
 * 계산 공식:
 * Volume Oscillator = [(Short MA - Long MA) / Long MA] × 100
 * 
 * 해석:
 * - 양수: 거래량 증가 추세 (단기 평균 > 장기 평균)
 * - 음수: 거래량 감소 추세 (단기 평균 < 장기 평균)
 * - 0 근처: 평균 수준의 거래량
 */
@Slf4j
@Component("VOLUME_OSCILLATOR")
public class VolumeOscillatorCalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_SHORT_PERIOD = 5;
    private static final int DEFAULT_LONG_PERIOD = 10;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        int shortPeriod = getIntParam(params, "shortPeriod", DEFAULT_SHORT_PERIOD);
        int longPeriod = getIntParam(params, "longPeriod", DEFAULT_LONG_PERIOD);
        
        // 1. 거래량 리스트 생성
        List<Double> volumes = new ArrayList<>();
        for (Candle candle : candles) {
            double volume = candle.getVolume() != null ? candle.getVolume().doubleValue() : 0.0;
            volumes.add(volume);
        }
        
        // 2. 단기/장기 이동평균 계산
        List<Double> shortMA = calculateSMA(volumes, shortPeriod);
        List<Double> longMA = calculateSMA(volumes, longPeriod);
        
        // 3. Volume Oscillator 계산
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        for (int i = 0; i < candles.size(); i++) {
            // ✅ 유효한 값이 있는 경우에만 계산
            if (Double.isNaN(shortMA.get(i)) || Double.isNaN(longMA.get(i)) || longMA.get(i) == 0) {
                continue;
            }
            
            double volumeOsc = ((shortMA.get(i) - longMA.get(i)) / longMA.get(i)) * 100.0;
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(Map.of(
                            "volumeOsc", volumeOsc,
                            "shortMA", shortMA.get(i),
                            "longMA", longMA.get(i)
                    ))
                    .build());
        }
        
        log.debug("[VOLUME_OSCILLATOR] Calculated {} data points with periods: {}/{}", 
                result.size(), shortPeriod, longPeriod);
        return result;
    }
    
    /**
     * SMA 계산
     */
    private List<Double> calculateSMA(List<Double> values, int period) {
        List<Double> sma = new ArrayList<>();
        
        for (int i = 0; i < values.size(); i++) {
            if (i < period - 1) {
                sma.add(Double.NaN);
            } else {
                double sum = 0.0;
                for (int j = i - period + 1; j <= i; j++) {
                    sum += values.get(j);
                }
                sma.add(sum / period);
            }
        }
        
        return sma;
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
