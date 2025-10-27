package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
