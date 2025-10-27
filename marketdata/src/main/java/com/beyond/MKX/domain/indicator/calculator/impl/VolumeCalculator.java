package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 거래량 (VOLUME) 계산기
 * 
 * 단순 거래량 표시 (하단 차트)
 */
@Slf4j
@Component("VOLUME")
public class VolumeCalculator implements IndicatorCalculator {
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        for (Candle candle : candles) {
            double volume = candle.getVolume() != null ? candle.getVolume().doubleValue() : 0.0;
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candle.getTime())
                    .values(Map.of("volume", volume))
                    .build());
        }
        
        log.debug("[VOLUME] Calculated {} data points", result.size());
        return result;
    }
    
    @Override
    public Map<String, Object> getDefaultParams() {
        return Collections.emptyMap();
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        return true;  // 파라미터 없음
    }
}
