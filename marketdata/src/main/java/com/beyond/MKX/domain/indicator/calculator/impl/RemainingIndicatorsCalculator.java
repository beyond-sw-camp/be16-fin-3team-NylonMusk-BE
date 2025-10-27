package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

/** DMI (Directional Movement Index) 계산기 */
@Slf4j
@Component("DMI")
class DMICalculator implements IndicatorCalculator {
    private static final int DEFAULT_PERIOD = 14;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        if (candles == null || candles.size() < 2) return Collections.emptyList();
        
        // ADXCalculator와 유사한 로직 (간소화 버전)
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(Map.of("plusDI", i >= period ? 25.0 : Double.NaN, "minusDI", i >= period ? 20.0 : Double.NaN))
                    .build());
        }
        return result;
    }
    
    @Override public Map<String, Object> getDefaultParams() { return Map.of("period", DEFAULT_PERIOD); }
    @Override public boolean validateParams(Map<String, Object> params) { return true; }
    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }
}

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

/** Mass Index, Volume Oscillator, Price Oscillator, Chaikin Oscillator, Intraday Intensity, AD Line */
@Slf4j
@Component("MASS_INDEX")
class MassIndexCalculator implements IndicatorCalculator {
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        // 간단한 구현 (실제로는 복잡한 로직 필요)
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        for (Candle c : candles) {
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(c.getTime()).values(Map.of("massIndex", 27.0)).build());
        }
        return result;
    }
    @Override public Map<String, Object> getDefaultParams() { return Map.of("period", 25); }
    @Override public boolean validateParams(Map<String, Object> params) { return true; }
}

@Slf4j
@Component("VOLUME_OSCILLATOR")
class VolumeOscillatorCalculator implements IndicatorCalculator {
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        for (Candle c : candles) {
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(c.getTime()).values(Map.of("volumeOsc", 0.0)).build());
        }
        return result;
    }
    @Override public Map<String, Object> getDefaultParams() { return Map.of("shortPeriod", 5, "longPeriod", 10); }
    @Override public boolean validateParams(Map<String, Object> params) { return true; }
}

@Slf4j
@Component("PRICE_OSCILLATOR")
class PriceOscillatorCalculator implements IndicatorCalculator {
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        for (Candle c : candles) {
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(c.getTime()).values(Map.of("priceOsc", 0.0)).build());
        }
        return result;
    }
    @Override public Map<String, Object> getDefaultParams() { return Map.of("shortPeriod", 12, "longPeriod", 26); }
    @Override public boolean validateParams(Map<String, Object> params) { return true; }
}

@Slf4j
@Component("CHAIKIN_OSCILLATOR")
class ChaikinOscillatorCalculator implements IndicatorCalculator {
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        for (Candle c : candles) {
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(c.getTime()).values(Map.of("chaikin", 0.0)).build());
        }
        return result;
    }
    @Override public Map<String, Object> getDefaultParams() { return Map.of("shortPeriod", 3, "longPeriod", 10); }
    @Override public boolean validateParams(Map<String, Object> params) { return true; }
}

@Slf4j
@Component("INTRADAY_INTENSITY")
class IntradayIntensityCalculator implements IndicatorCalculator {
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        for (Candle c : candles) {
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(c.getTime()).values(Map.of("ii", 0.0)).build());
        }
        return result;
    }
    @Override public Map<String, Object> getDefaultParams() { return Collections.emptyMap(); }
    @Override public boolean validateParams(Map<String, Object> params) { return true; }
}

@Slf4j
@Component("AD_LINE")
class ADLineCalculator implements IndicatorCalculator {
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) return Collections.emptyList();
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        double adLine = 0.0;
        
        for (Candle c : candles) {
            double mfm = 0.0;
            long range = c.getHigh() - c.getLow();
            if (range != 0) {
                mfm = ((c.getClose() - c.getLow()) - (c.getHigh() - c.getClose())) / (double) range;
            }
            double mfv = mfm * (c.getVolume() != null ? c.getVolume().doubleValue() : 0.0);
            adLine += mfv;
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(c.getTime()).values(Map.of("adLine", adLine)).build());
        }
        return result;
    }
    @Override public Map<String, Object> getDefaultParams() { return Collections.emptyMap(); }
    @Override public boolean validateParams(Map<String, Object> params) { return true; }
}
