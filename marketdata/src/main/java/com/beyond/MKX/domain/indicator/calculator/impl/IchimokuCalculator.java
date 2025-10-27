package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 일목균형표 (Ichimoku Cloud) 계산기
 * 
 * 5개 선으로 구성된 복잡한 지표:
 * - 전환선 (Tenkan-sen): 9일 중간값
 * - 기준선 (Kijun-sen): 26일 중간값
 * - 선행스팬1 (Senkou Span A): (전환선 + 기준선) / 2, 26일 선행
 * - 선행스팬2 (Senkou Span B): 52일 중간값, 26일 선행
 * - 후행스팬 (Chikou Span): 현재 종가, 26일 후행
 */
@Slf4j
@Component("ICHIMOKU")
public class IchimokuCalculator implements IndicatorCalculator {
    
    private static final int TENKAN_PERIOD = 9;      // 전환선
    private static final int KIJUN_PERIOD = 26;      // 기준선
    private static final int SENKOU_B_PERIOD = 52;   // 선행스팬2
    private static final int DISPLACEMENT = 26;      // 선행/후행 기간
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        int tenkanPeriod = getIntParam(params, "tenkanPeriod", TENKAN_PERIOD);
        int kijunPeriod = getIntParam(params, "kijunPeriod", KIJUN_PERIOD);
        int senkouBPeriod = getIntParam(params, "senkouBPeriod", SENKOU_B_PERIOD);
        int displacement = getIntParam(params, "displacement", DISPLACEMENT);
        
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        // 전환선 계산
        List<Double> tenkanSen = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            if (i < tenkanPeriod - 1) {
                tenkanSen.add(Double.NaN);
            } else {
                double midPoint = calculateMidPoint(candles, i - tenkanPeriod + 1, i);
                tenkanSen.add(midPoint);
            }
        }
        
        // 기준선 계산
        List<Double> kijunSen = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            if (i < kijunPeriod - 1) {
                kijunSen.add(Double.NaN);
            } else {
                double midPoint = calculateMidPoint(candles, i - kijunPeriod + 1, i);
                kijunSen.add(midPoint);
            }
        }
        
        // 선행스팬 A (전환선 + 기준선) / 2
        List<Double> senkouSpanA = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            if (Double.isNaN(tenkanSen.get(i)) || Double.isNaN(kijunSen.get(i))) {
                senkouSpanA.add(Double.NaN);
            } else {
                senkouSpanA.add((tenkanSen.get(i) + kijunSen.get(i)) / 2);
            }
        }
        
        // 선행스팬 B
        List<Double> senkouSpanB = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            if (i < senkouBPeriod - 1) {
                senkouSpanB.add(Double.NaN);
            } else {
                double midPoint = calculateMidPoint(candles, i - senkouBPeriod + 1, i);
                senkouSpanB.add(midPoint);
            }
        }
        
        // 후행스팬 (현재 종가)
        List<Double> chikouSpan = new ArrayList<>();
        for (Candle candle : candles) {
            chikouSpan.add((double) candle.getClose());
        }
        
        // 결과 생성
        for (int i = 0; i < candles.size(); i++) {
            Map<String, Double> values = new HashMap<>();
            
            values.put("tenkanSen", tenkanSen.get(i));
            values.put("kijunSen", kijunSen.get(i));
            
            // 선행스팬 (26일 앞으로 이동)
            if (i >= displacement) {
                values.put("senkouSpanA", senkouSpanA.get(i - displacement));
                values.put("senkouSpanB", senkouSpanB.get(i - displacement));
            } else {
                values.put("senkouSpanA", Double.NaN);
                values.put("senkouSpanB", Double.NaN);
            }
            
            // 후행스팬 (26일 뒤로 이동)
            if (i + displacement < candles.size()) {
                values.put("chikouSpan", chikouSpan.get(i + displacement));
            } else {
                values.put("chikouSpan", Double.NaN);
            }
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(values)
                    .build());
        }
        
        log.debug("[ICHIMOKU] Calculated {} data points", result.size());
        return result;
    }
    
    /**
     * 지정된 기간의 중간값 계산 (최고가 + 최저가) / 2
     */
    private double calculateMidPoint(List<Candle> candles, int start, int end) {
        long high = Long.MIN_VALUE;
        long low = Long.MAX_VALUE;
        
        for (int i = start; i <= end; i++) {
            if (candles.get(i).getHigh() > high) {
                high = candles.get(i).getHigh();
            }
            if (candles.get(i).getLow() < low) {
                low = candles.get(i).getLow();
            }
        }
        
        return (high + low) / 2.0;
    }
    
    @Override
    public Map<String, Object> getDefaultParams() {
        return Map.of(
                "tenkanPeriod", TENKAN_PERIOD,
                "kijunPeriod", KIJUN_PERIOD,
                "senkouBPeriod", SENKOU_B_PERIOD,
                "displacement", DISPLACEMENT
        );
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        try {
            int tenkan = getIntParam(params, "tenkanPeriod", TENKAN_PERIOD);
            int kijun = getIntParam(params, "kijunPeriod", KIJUN_PERIOD);
            int senkouB = getIntParam(params, "senkouBPeriod", SENKOU_B_PERIOD);
            int displacement = getIntParam(params, "displacement", DISPLACEMENT);
            
            return tenkan > 0 && kijun > 0 && senkouB > 0 && displacement > 0
                    && tenkan < kijun && kijun < senkouB;
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
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        return defaultValue;
    }
}
