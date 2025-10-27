package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** 볼린저밴드 %B 계산기 */
@Slf4j
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

