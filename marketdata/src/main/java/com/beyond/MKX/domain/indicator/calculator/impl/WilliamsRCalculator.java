package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * 윌리엄스 %R (Williams %R) 계산기
 * 과매수/과매도 오실레이터 (-100 ~ 0)
 */
@Slf4j
@Component("WILLIAMS_R")
public class WilliamsRCalculator implements IndicatorCalculator {
    private static final int DEFAULT_PERIOD = 14;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) return Collections.emptyList();
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        for (int i = 0; i < candles.size(); i++) {
            if (i < period - 1) {
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime()).values(Map.of("williamsR", Double.NaN)).build());
                continue;
            }
            long highest = Long.MIN_VALUE, lowest = Long.MAX_VALUE;
            for (int j = i - period + 1; j <= i; j++) {
                highest = Math.max(highest, candles.get(j).getHigh());
                lowest = Math.min(lowest, candles.get(j).getLow());
            }
            double williamsR = highest != lowest ? ((highest - candles.get(i).getClose()) / (double)(highest - lowest)) * -100.0 : 0.0;
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime()).values(Map.of("williamsR", williamsR)).build());
        }
        return result;
    }
    
    @Override public Map<String, Object> getDefaultParams() { return Map.of("period", DEFAULT_PERIOD); }
    @Override public boolean validateParams(Map<String, Object> params) {
        try { int period = getIntParam(params, "period", DEFAULT_PERIOD); return period > 0 && period <= 100; }
        catch (Exception e) { return false; }
    }
    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        if (value instanceof Integer) return (Integer) value;
        else if (value instanceof Number) return ((Number) value).intValue();
        return defaultValue;
    }
}
