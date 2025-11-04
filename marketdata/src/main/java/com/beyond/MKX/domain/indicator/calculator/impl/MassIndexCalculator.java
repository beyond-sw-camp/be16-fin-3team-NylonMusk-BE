package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Mass Index 계산기
 * 
 * 추세 반전을 예측하는 변동성 지표
 * 고가-저가 범위의 EMA를 이용하여 계산
 * 
 * 계산 공식:
 * 1. Single EMA = EMA(9, High-Low)
 * 2. Double EMA = EMA(9, Single EMA)
 * 3. EMA Ratio = Single EMA / Double EMA
 * 4. Mass Index = Sum(25, EMA Ratio)
 * 
 * 해석:
 * - 27 이상: 추세 반전 가능성 높음
 * - 26.5 미만: 정상 범위
 */
@Slf4j
@Component("MASS_INDEX")
public class MassIndexCalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_EMA_PERIOD = 9;
    private static final int DEFAULT_SUM_PERIOD = 25;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        int emaPeriod = getIntParam(params, "emaPeriod", DEFAULT_EMA_PERIOD);
        int sumPeriod = getIntParam(params, "sumPeriod", DEFAULT_SUM_PERIOD);
        
        // 1. High-Low 범위 계산
        List<Double> ranges = new ArrayList<>();
        for (Candle candle : candles) {
            double range = candle.getHigh() - candle.getLow();
            ranges.add(range);
        }
        
        // 2. Single EMA 계산
        List<Double> singleEMA = calculateEMA(ranges, emaPeriod);
        
        // 3. Double EMA 계산 (EMA of EMA)
        List<Double> doubleEMA = calculateEMA(singleEMA, emaPeriod);
        
        // 4. EMA Ratio 계산
        List<Double> emaRatio = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            if (Double.isNaN(singleEMA.get(i)) || Double.isNaN(doubleEMA.get(i)) || doubleEMA.get(i) == 0) {
                emaRatio.add(Double.NaN);
            } else {
                emaRatio.add(singleEMA.get(i) / doubleEMA.get(i));
            }
        }
        
        // 5. Mass Index 계산 (EMA Ratio의 합계)
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        for (int i = 0; i < candles.size(); i++) {
            if (i < sumPeriod - 1) {
                // ✅ 데이터가 부족한 경우 건너뛰기
                continue;
            }
            
            // sumPeriod 기간 동안의 EMA Ratio 합계
            double massIndex = 0.0;
            boolean hasValidData = true;
            
            for (int j = i - sumPeriod + 1; j <= i; j++) {
                if (Double.isNaN(emaRatio.get(j))) {
                    hasValidData = false;
                    break;
                }
                massIndex += emaRatio.get(j);
            }
            
            // ✅ 유효한 데이터가 있는 경우에만 추가
            if (!hasValidData) {
                continue;
            }
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(Map.of("massIndex", massIndex))
                    .build());
        }
        
        log.debug("[MASS_INDEX] Calculated {} data points with emaPeriod={}, sumPeriod={}", 
                result.size(), emaPeriod, sumPeriod);
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
                "emaPeriod", DEFAULT_EMA_PERIOD,
                "sumPeriod", DEFAULT_SUM_PERIOD
        );
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        try {
            int emaPeriod = getIntParam(params, "emaPeriod", DEFAULT_EMA_PERIOD);
            int sumPeriod = getIntParam(params, "sumPeriod", DEFAULT_SUM_PERIOD);
            
            return emaPeriod > 0 && emaPeriod <= 50 && sumPeriod > 0 && sumPeriod <= 100;
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
