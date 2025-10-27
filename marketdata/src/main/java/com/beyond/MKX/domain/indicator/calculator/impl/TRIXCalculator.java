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

/** TRIX 계산기 - 3중 지수 이동평균의 변화율 */
@Slf4j
@Component("TRIX")
class TRIXCalculator implements IndicatorCalculator {
    private static final int DEFAULT_PERIOD = 15;

    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) return Collections.emptyList();
        int period = getIntParam(params, "period", DEFAULT_PERIOD);

        // 1차 EMA
        List<Double> ema1 = calculateEMA(candles, period);
        // 2차 EMA (EMA of EMA)
        List<Double> ema2 = calculateEMAFromValues(ema1, period);
        // 3차 EMA
        List<Double> ema3 = calculateEMAFromValues(ema2, period);

        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(candles.get(0).getTime()).values(Map.of("trix", Double.NaN)).build());

        for (int i = 1; i < candles.size(); i++) {
            double trix = Double.NaN;
            if (!Double.isNaN(ema3.get(i)) && !Double.isNaN(ema3.get(i-1)) && ema3.get(i-1) != 0) {
                trix = ((ema3.get(i) - ema3.get(i-1)) / ema3.get(i-1)) * 100.0;
            }
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime()).values(Map.of("trix", trix)).build());
        }
        return result;
    }

    private List<Double> calculateEMA(List<Candle> candles, int period) {
        List<Double> ema = new ArrayList<>();
        double multiplier = 2.0 / (period + 1);
        double currentEMA = 0.0;
        for (int i = 0; i < period && i < candles.size(); i++) currentEMA += candles.get(i).getClose();
        currentEMA /= Math.min(period, candles.size());

        for (int i = 0; i < candles.size(); i++) {
            if (i < period - 1) ema.add(Double.NaN);
            else if (i == period - 1) ema.add(currentEMA);
            else {
                currentEMA = (candles.get(i).getClose() * multiplier) + (currentEMA * (1 - multiplier));
                ema.add(currentEMA);
            }
        }
        return ema;
    }

    private List<Double> calculateEMAFromValues(List<Double> values, int period) {
        List<Double> ema = new ArrayList<>();
        double multiplier = 2.0 / (period + 1);
        int firstValid = -1;
        for (int i = 0; i < values.size(); i++) {
            if (!Double.isNaN(values.get(i))) { firstValid = i; break; }
        }
        if (firstValid == -1) return Collections.nCopies(values.size(), Double.NaN);

        double currentEMA = 0.0;
        int count = 0;
        for (int i = firstValid; i < firstValid + period && i < values.size(); i++) {
            if (!Double.isNaN(values.get(i))) { currentEMA += values.get(i); count++; }
        }
        currentEMA /= count;

        for (int i = 0; i < values.size(); i++) {
            if (i < firstValid + period - 1) ema.add(Double.NaN);
            else if (i == firstValid + period - 1) ema.add(currentEMA);
            else {
                if (!Double.isNaN(values.get(i))) {
                    currentEMA = (values.get(i) * multiplier) + (currentEMA * (1 - multiplier));
                    ema.add(currentEMA);
                } else ema.add(Double.NaN);
            }
        }
        return ema;
    }

    @Override public Map<String, Object> getDefaultParams() { return Map.of("period", DEFAULT_PERIOD); }
    @Override public boolean validateParams(Map<String, Object> params) { return true; }
    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }
}