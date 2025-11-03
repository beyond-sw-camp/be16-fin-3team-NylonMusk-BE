package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Intraday Intensity Index 계산기
 * 
 * 일중 매수/매도 압력을 측정하는 지표
 * 종가가 고가/저가 중 어디에 가까운지와 거래량을 고려
 * 
 * 계산 공식:
 * II = (2 × Close - High - Low) / (High - Low) × Volume
 * 
 * 해석:
 * - 양수: 매수 압력 우세 (종가가 고가에 가까움)
 * - 음수: 매도 압력 우세 (종가가 저가에 가까움)
 */
@Slf4j
@Component("INTRADAY_INTENSITY")
public class IntradayIntensityCalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_PERIOD = 21;  // 누적 기간
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        int period = getIntParam(params, "period", DEFAULT_PERIOD);
        
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        // 각 캔들의 II 값 계산
        List<Double> iiValues = new ArrayList<>();
        for (Candle candle : candles) {
            double high = candle.getHigh();
            double low = candle.getLow();
            double close = candle.getClose();
            double volume = candle.getVolume() != null ? candle.getVolume().doubleValue() : 0.0;
            
            double ii = 0.0;
            if (high != low) {
                ii = ((2 * close - high - low) / (high - low)) * volume;
            }
            
            iiValues.add(ii);
        }
        
        // 누적 II 계산 (Moving Sum)
        for (int i = 0; i < candles.size(); i++) {
            if (i < period - 1) {
                // ✅ 데이터가 부족한 경우 건너뛰기
                continue;
            }
            
            // period 기간 동안의 II 합계
            double sumII = 0.0;
            for (int j = i - period + 1; j <= i; j++) {
                sumII += iiValues.get(j);
            }
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(Map.of(
                            "ii", sumII,
                            "iiSingle", iiValues.get(i)  // 개별 캔들의 II 값도 제공
                    ))
                    .build());
        }
        
        log.debug("[INTRADAY_INTENSITY] Calculated {} data points with period={}", result.size(), period);
        return result;
    }
    
    @Override
    public Map<String, Object> getDefaultParams() {
        return Map.of("period", DEFAULT_PERIOD);
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        try {
            int period = getIntParam(params, "period", DEFAULT_PERIOD);
            return period > 0 && period <= 100;
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
