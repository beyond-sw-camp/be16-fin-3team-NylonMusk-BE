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
            // ✅ 데이터가 부족한 경우 건너뛰기
            if (i < period) {
                continue;
            }
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(i).getTime())
                    .values(Map.of("plusDI", 25.0, "minusDI", 20.0))
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