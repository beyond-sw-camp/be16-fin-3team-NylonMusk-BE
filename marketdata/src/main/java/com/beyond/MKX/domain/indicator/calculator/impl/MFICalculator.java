package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/** MFI (Money Flow Index) 계산기 */
@Slf4j
@Component("MFI")
public class MFICalculator implements IndicatorCalculator {
    private static final int DEFAULT_PERIOD = 14;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.size() < 2) return Collections.emptyList();
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        List<Double> typicalPrices = new ArrayList<>();
        List<Double> rawMoneyFlow = new ArrayList<>();
        
        for (Candle c : candles) {
            double tp = (c.getHigh() + c.getLow() + c.getClose()) / 3.0;
            typicalPrices.add(tp);
            double rmf = tp * (c.getVolume() != null ? c.getVolume().doubleValue() : 0.0);
            rawMoneyFlow.add(rmf);
        }
        
        result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(candles.get(0).getTime()).values(Map.of("mfi", Double.NaN)).build());
        
        for (int i = 1; i < candles.size(); i++) {
            if (i < period) {
                result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                        .time(candles.get(i).getTime()).values(Map.of("mfi", Double.NaN)).build());
                continue;
            }
            double positiveFlow = 0.0, negativeFlow = 0.0;
            for (int j = i - period + 1; j <= i; j++) {
                if (typicalPrices.get(j) > typicalPrices.get(j - 1)) positiveFlow += rawMoneyFlow.get(j);
                else if (typicalPrices.get(j) < typicalPrices.get(j - 1)) negativeFlow += rawMoneyFlow.get(j);
            }
            double mfi = negativeFlow != 0 ? 100.0 - (100.0 / (1.0 + (positiveFlow / negativeFlow))) : 100.0;
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime()).values(Map.of("mfi", mfi)).build());
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
