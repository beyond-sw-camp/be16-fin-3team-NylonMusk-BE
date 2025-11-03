package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/** CCI (Commodity Channel Index) 계산기 */
@Slf4j
@Component("CCI")
public class CCICalculator implements IndicatorCalculator {
    private static final int DEFAULT_PERIOD = 20;
    private static final double CONSTANT = 0.015;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) return Collections.emptyList();
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        List<Double> typicalPrices = new ArrayList<>();
        for (Candle c : candles) typicalPrices.add((c.getHigh() + c.getLow() + c.getClose()) / 3.0);
        
        for (int i = 0; i < candles.size(); i++) {
            if (i < period - 1) {
                // ✅ 데이터가 부족한 경우 건너뛰기
                continue;
            }
            double sma = 0.0;
            for (int j = i - period + 1; j <= i; j++) sma += typicalPrices.get(j);
            sma /= period;
            double meanDeviation = 0.0;
            for (int j = i - period + 1; j <= i; j++) meanDeviation += Math.abs(typicalPrices.get(j) - sma);
            meanDeviation /= period;
            double cci = meanDeviation != 0 ? (typicalPrices.get(i) - sma) / (CONSTANT * meanDeviation) : 0.0;
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime()).values(Map.of("cci", cci)).build());
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
