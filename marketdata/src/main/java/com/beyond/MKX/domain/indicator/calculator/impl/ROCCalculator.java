package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/** ROC (Rate of Change) 계산기 */
@Slf4j
@Component("ROC")
public class ROCCalculator implements IndicatorCalculator {
    private static final int DEFAULT_PERIOD = 12;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) return Collections.emptyList();
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        for (int i = 0; i < candles.size(); i++) {
            if (i < period) {
                // ✅ 데이터가 부족한 경우 건너뛰기
                continue;
            }
            long prevClose = candles.get(i - period).getClose();
            double roc = prevClose != 0 ? ((candles.get(i).getClose() - prevClose) / (double) prevClose) * 100.0 : 0.0;
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime()).values(Map.of("roc", roc)).build());
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
