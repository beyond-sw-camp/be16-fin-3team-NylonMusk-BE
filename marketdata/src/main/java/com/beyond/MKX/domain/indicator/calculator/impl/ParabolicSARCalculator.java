package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 파라볼릭 SAR (Parabolic Stop and Reverse) 계산기
 * 
 * 추세 전환을 포착하는 지표
 * - 상승 추세: 가격 아래에 점으로 표시
 * - 하락 추세: 가격 위에 점으로 표시
 */
@Slf4j
@Component("PARABOLIC_SAR")
public class ParabolicSARCalculator implements IndicatorCalculator {
    
    private static final double DEFAULT_ACCELERATION = 0.02;
    private static final double DEFAULT_MAX_ACCELERATION = 0.20;
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.size() < 2) {
            return Collections.emptyList();
        }
        
        double acceleration = getDoubleParam(params, "acceleration", DEFAULT_ACCELERATION);
        double maxAcceleration = getDoubleParam(params, "maxAcceleration", DEFAULT_MAX_ACCELERATION);
        
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        // 초기 설정
        boolean isUpTrend = candles.get(1).getClose() > candles.get(0).getClose();
        double sar = isUpTrend ? candles.get(0).getLow() : candles.get(0).getHigh();
        double ep = isUpTrend ? candles.get(0).getHigh() : candles.get(0).getLow();  // Extreme Point
        double af = acceleration;  // Acceleration Factor
        
        result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(candles.get(0).getTime())
                .values(Map.of("sar", sar, "trend", isUpTrend ? 1.0 : -1.0))
                .build());
        
        for (int i = 1; i < candles.size(); i++) {
            Candle candle = candles.get(i);
            
            // SAR 계산
            sar = sar + af * (ep - sar);
            
            // 추세 전환 확인
            boolean trendChanged = false;
            
            if (isUpTrend) {
                // 상승 추세에서 SAR이 현재 저가보다 높으면 추세 전환
                if (sar > candle.getLow()) {
                    trendChanged = true;
                    isUpTrend = false;
                    sar = ep;  // SAR을 이전 EP로 설정
                    ep = candle.getLow();
                    af = acceleration;
                } else {
                    // 새로운 고가 갱신 시 EP와 AF 업데이트
                    if (candle.getHigh() > ep) {
                        ep = candle.getHigh();
                        af = Math.min(af + acceleration, maxAcceleration);
                    }
                }
            } else {
                // 하락 추세에서 SAR이 현재 고가보다 낮으면 추세 전환
                if (sar < candle.getHigh()) {
                    trendChanged = true;
                    isUpTrend = true;
                    sar = ep;  // SAR을 이전 EP로 설정
                    ep = candle.getHigh();
                    af = acceleration;
                } else {
                    // 새로운 저가 갱신 시 EP와 AF 업데이트
                    if (candle.getLow() < ep) {
                        ep = candle.getLow();
                        af = Math.min(af + acceleration, maxAcceleration);
                    }
                }
            }
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candle.getTime())
                    .values(Map.of(
                            "sar", sar,
                            "trend", isUpTrend ? 1.0 : -1.0,
                            "ep", ep,
                            "af", af
                    ))
                    .build());
        }
        
        log.debug("[PARABOLIC_SAR] Calculated {} data points", result.size());
        return result;
    }
    
    @Override
    public Map<String, Object> getDefaultParams() {
        return Map.of(
                "acceleration", DEFAULT_ACCELERATION,
                "maxAcceleration", DEFAULT_MAX_ACCELERATION
        );
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        try {
            double acceleration = getDoubleParam(params, "acceleration", DEFAULT_ACCELERATION);
            double maxAcceleration = getDoubleParam(params, "maxAcceleration", DEFAULT_MAX_ACCELERATION);
            
            return acceleration > 0 && acceleration <= maxAcceleration && maxAcceleration <= 1.0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = params.get(key);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        return defaultValue;
    }
}
