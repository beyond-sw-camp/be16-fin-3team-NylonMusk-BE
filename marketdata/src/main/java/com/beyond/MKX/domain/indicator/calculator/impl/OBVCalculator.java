package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * OBV (On Balance Volume) 계산기
 * 
 * 거래량 기반 모멘텀 지표
 * - 종가 상승 시: OBV += Volume
 * - 종가 하락 시: OBV -= Volume
 * - 종가 동일 시: OBV 유지
 */
@Slf4j
@Component("OBV")
public class OBVCalculator implements IndicatorCalculator {
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        BigDecimal obv = BigDecimal.ZERO;
        
        // 첫 번째 캔들
        if (candles.get(0).getVolume() != null) {
            obv = candles.get(0).getVolume();
        }
        
        result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(candles.get(0).getTime())
                .values(Map.of("obv", obv.doubleValue()))
                .build());
        
        // 나머지 캔들
        for (int i = 1; i < candles.size(); i++) {
            Candle current = candles.get(i);
            Candle previous = candles.get(i - 1);
            
            if (current.getVolume() != null) {
                if (current.getClose() > previous.getClose()) {
                    obv = obv.add(current.getVolume());
                } else if (current.getClose() < previous.getClose()) {
                    obv = obv.subtract(current.getVolume());
                }
                // 종가가 같으면 OBV 유지
            }
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(current.getTime())
                    .values(Map.of("obv", obv.doubleValue()))
                    .build());
        }
        
        log.debug("[OBV] Calculated {} data points", result.size());
        return result;
    }
    
    @Override
    public Map<String, Object> getDefaultParams() {
        return Collections.emptyMap();
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        return true;
    }
}
