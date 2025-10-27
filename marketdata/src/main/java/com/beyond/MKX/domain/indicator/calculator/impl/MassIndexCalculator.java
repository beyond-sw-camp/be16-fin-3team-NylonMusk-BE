package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
