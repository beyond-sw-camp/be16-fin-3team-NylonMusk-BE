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
