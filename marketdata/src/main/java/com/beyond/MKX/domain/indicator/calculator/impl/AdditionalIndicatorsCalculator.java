package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

/** ADX (Average Directional Index) 계산기 - 추세 강도 측정 */
@Slf4j
@Component("ADX")
class ADXCalculator implements IndicatorCalculator {
    private static final int DEFAULT_PERIOD = 14;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.size() < 2) return Collections.emptyList();
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        // +DM, -DM, TR 계산
        List<Double> plusDM = new ArrayList<>(), minusDM = new ArrayList<>(), tr = new ArrayList<>();
        result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(candles.get(0).getTime()).values(Map.of("adx", Double.NaN, "plusDI", Double.NaN, "minusDI", Double.NaN)).build());
        
        for (int i = 1; i < candles.size(); i++) {
            double highDiff = candles.get(i).getHigh() - candles.get(i-1).getHigh();
            double lowDiff = candles.get(i-1).getLow() - candles.get(i).getLow();
            plusDM.add(highDiff > lowDiff && highDiff > 0 ? highDiff : 0.0);
            minusDM.add(lowDiff > highDiff && lowDiff > 0 ? lowDiff : 0.0);
            double trValue = Math.max(candles.get(i).getHigh() - candles.get(i).getLow(),
                    Math.max(Math.abs(candles.get(i).getHigh() - candles.get(i-1).getClose()),
                            Math.abs(candles.get(i).getLow() - candles.get(i-1).getClose())));
            tr.add(trValue);
        }
        
        // Smoothed +DM, -DM, TR
        double smoothPlusDM = 0.0, smoothMinusDM = 0.0, smoothTR = 0.0;
        for (int i = 0; i < period && i < plusDM.size(); i++) {
            smoothPlusDM += plusDM.get(i); smoothMinusDM += minusDM.get(i); smoothTR += tr.get(i);
        }
        
        List<Double> dx = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            if (i < period) {
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime()).values(Map.of("adx", Double.NaN, "plusDI", Double.NaN, "minusDI", Double.NaN)).build());
                dx.add(Double.NaN);
                continue;
            }
            if (i > period) {
                smoothPlusDM = smoothPlusDM - (smoothPlusDM / period) + plusDM.get(i-1);
                smoothMinusDM = smoothMinusDM - (smoothMinusDM / period) + minusDM.get(i-1);
                smoothTR = smoothTR - (smoothTR / period) + tr.get(i-1);
            }
            double plusDI = smoothTR != 0 ? (smoothPlusDM / smoothTR) * 100.0 : 0.0;
            double minusDI = smoothTR != 0 ? (smoothMinusDM / smoothTR) * 100.0 : 0.0;
            double dxValue = (plusDI + minusDI) != 0 ? Math.abs(plusDI - minusDI) / (plusDI + minusDI) * 100.0 : 0.0;
            dx.add(dxValue);
            
            double adx = Double.NaN;
            if (i >= period * 2 - 1) {
                double sum = 0.0;
                for (int j = i - period + 1; j <= i && j - period + 1 < dx.size(); j++) {
                    if (!Double.isNaN(dx.get(j - period + 1))) sum += dx.get(j - period + 1);
                }
                adx = sum / period;
            }
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime()).values(Map.of("adx", adx, "plusDI", plusDI, "minusDI", minusDI)).build());
        }
        return result;
    }
    
    @Override public Map<String, Object> getDefaultParams() { return Map.of("period", DEFAULT_PERIOD); }
    @Override public boolean validateParams(Map<String, Object> params) {
        try { return getIntParam(params, "period", DEFAULT_PERIOD) > 0; } catch (Exception e) { return false; }
    }
    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }
}

/** 모멘텀 (Momentum) 계산기 */
@Slf4j
@Component("MOMENTUM")
class MomentumCalculator implements IndicatorCalculator {
    private static final int DEFAULT_PERIOD = 10;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) return Collections.emptyList();
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        for (int i = 0; i < candles.size(); i++) {
            if (i < period) {
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime()).values(Map.of("momentum", Double.NaN)).build());
            } else {
                double momentum = candles.get(i).getClose() - candles.get(i - period).getClose();
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime()).values(Map.of("momentum", momentum)).build());
            }
        }
        return result;
    }
    
    @Override public Map<String, Object> getDefaultParams() { return Map.of("period", DEFAULT_PERIOD); }
    @Override public boolean validateParams(Map<String, Object> params) {
        try { return getIntParam(params, "period", DEFAULT_PERIOD) > 0; } catch (Exception e) { return false; }
    }
    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }
}

/** 볼린저밴드 %B 계산기 */
@Slf4j
@Component("BOLLINGER_B")
class BollingerBCalculator implements IndicatorCalculator {
    private static final int DEFAULT_PERIOD = 20;
    private static final double DEFAULT_STD_DEV = 2.0;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) return Collections.emptyList();
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        double stdDevMult = getDoubleParam(params, "stdDev", DEFAULT_STD_DEV);
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        for (int i = 0; i < candles.size(); i++) {
            if (i < period - 1) {
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime()).values(Map.of("percentB", Double.NaN)).build());
                continue;
            }
            double sum = 0.0;
            for (int j = i - period + 1; j <= i; j++) sum += candles.get(j).getClose();
            double sma = sum / period;
            double variance = 0.0;
            for (int j = i - period + 1; j <= i; j++) {
                double diff = candles.get(j).getClose() - sma;
                variance += diff * diff;
            }
            double stdDev = Math.sqrt(variance / period);
            double upper = sma + (stdDev * stdDevMult);
            double lower = sma - (stdDev * stdDevMult);
            double percentB = (upper != lower) ? (candles.get(i).getClose() - lower) / (upper - lower) : 0.5;
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime()).values(Map.of("percentB", percentB)).build());
        }
        return result;
    }
    
    @Override public Map<String, Object> getDefaultParams() { return Map.of("period", DEFAULT_PERIOD, "stdDev", DEFAULT_STD_DEV); }
    @Override public boolean validateParams(Map<String, Object> params) { return true; }
    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }
    private double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : defaultValue;
    }
}

/** 볼린저밴드폭 계산기 */
@Slf4j
@Component("BOLLINGER_WIDTH")
class BollingerWidthCalculator implements IndicatorCalculator {
    private static final int DEFAULT_PERIOD = 20;
    private static final double DEFAULT_STD_DEV = 2.0;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) return Collections.emptyList();
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        double stdDevMult = getDoubleParam(params, "stdDev", DEFAULT_STD_DEV);
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        for (int i = 0; i < candles.size(); i++) {
            if (i < period - 1) {
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime()).values(Map.of("width", Double.NaN)).build());
                continue;
            }
            double sum = 0.0;
            for (int j = i - period + 1; j <= i; j++) sum += candles.get(j).getClose();
            double sma = sum / period;
            double variance = 0.0;
            for (int j = i - period + 1; j <= i; j++) {
                double diff = candles.get(j).getClose() - sma;
                variance += diff * diff;
            }
            double stdDev = Math.sqrt(variance / period);
            double width = sma != 0 ? ((stdDev * stdDevMult * 2) / sma) * 100.0 : 0.0;
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime()).values(Map.of("width", width)).build());
        }
        return result;
    }
    
    @Override public Map<String, Object> getDefaultParams() { return Map.of("period", DEFAULT_PERIOD, "stdDev", DEFAULT_STD_DEV); }
    @Override public boolean validateParams(Map<String, Object> params) { return true; }
    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }
    private double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : defaultValue;
    }
}
