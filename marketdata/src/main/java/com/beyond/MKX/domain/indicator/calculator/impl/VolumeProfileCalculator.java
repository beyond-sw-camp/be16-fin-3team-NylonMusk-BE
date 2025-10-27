package com.beyond.MKX.domain.indicator.calculator.impl;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.calculator.IndicatorCalculator;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 매물대 분석 (Volume Profile) 계산기
 * 
 * 가격대별 거래량 분포를 분석하여 지지/저항 구간 파악
 * - POC (Point of Control): 최대 거래량 가격대
 * - VAH (Value Area High): 상위 70% 거래량 구간의 최고가
 * - VAL (Value Area Low): 상위 70% 거래량 구간의 최저가
 */
@Slf4j
@Component("VOLUME_PROFILE")
public class VolumeProfileCalculator implements IndicatorCalculator {
    
    private static final int DEFAULT_ROWS = 24;  // 가격대 분할 개수
    private static final double VALUE_AREA_PERCENTAGE = 0.70;  // 70%
    
    @Override
    public List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        
        int rows = getIntParam(params, "rows", DEFAULT_ROWS);
        double valueAreaPct = getDoubleParam(params, "valueAreaPercentage", VALUE_AREA_PERCENTAGE);
        
        // 전체 가격 범위 계산
        long minPrice = candles.stream().mapToLong(Candle::getLow).min().orElse(0);
        long maxPrice = candles.stream().mapToLong(Candle::getHigh).max().orElse(0);
        
        if (minPrice == maxPrice) {
            return Collections.emptyList();
        }
        
        // 가격대 분할
        double priceStep = (maxPrice - minPrice) / (double) rows;
        
        // 가격대별 거래량 집계
        Map<Integer, BigDecimal> volumeProfile = new TreeMap<>();
        for (int i = 0; i < rows; i++) {
            volumeProfile.put(i, BigDecimal.ZERO);
        }
        
        for (Candle candle : candles) {
            // 캔들의 평균 가격으로 해당 가격대 찾기
            double avgPrice = (candle.getHigh() + candle.getLow() + candle.getClose()) / 3.0;
            int rowIndex = (int) Math.min(rows - 1, (avgPrice - minPrice) / priceStep);
            
            if (rowIndex >= 0 && rowIndex < rows && candle.getVolume() != null) {
                volumeProfile.put(rowIndex, volumeProfile.get(rowIndex).add(candle.getVolume()));
            }
        }
        
        // POC (Point of Control) 찾기 - 최대 거래량 가격대
        int pocIndex = volumeProfile.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);
        
        double pocPrice = minPrice + (pocIndex + 0.5) * priceStep;
        
        // Value Area 계산 (70% 거래량 포함 구간)
        BigDecimal totalVolume = volumeProfile.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal targetVolume = totalVolume.multiply(BigDecimal.valueOf(valueAreaPct));
        
        // POC에서 시작하여 좌우로 확장
        Set<Integer> valueArea = new HashSet<>();
        valueArea.add(pocIndex);
        BigDecimal currentVolume = volumeProfile.get(pocIndex);
        
        int upperIndex = pocIndex + 1;
        int lowerIndex = pocIndex - 1;
        
        while (currentVolume.compareTo(targetVolume) < 0 && (upperIndex < rows || lowerIndex >= 0)) {
            BigDecimal upperVolume = (upperIndex < rows) ? volumeProfile.get(upperIndex) : BigDecimal.ZERO;
            BigDecimal lowerVolume = (lowerIndex >= 0) ? volumeProfile.get(lowerIndex) : BigDecimal.ZERO;
            
            if (upperVolume.compareTo(lowerVolume) >= 0 && upperIndex < rows) {
                valueArea.add(upperIndex);
                currentVolume = currentVolume.add(upperVolume);
                upperIndex++;
            } else if (lowerIndex >= 0) {
                valueArea.add(lowerIndex);
                currentVolume = currentVolume.add(lowerVolume);
                lowerIndex--;
            } else {
                break;
            }
        }
        
        // VAH, VAL 계산
        int vahIndex = valueArea.stream().max(Integer::compare).orElse(pocIndex);
        int valIndex = valueArea.stream().min(Integer::compare).orElse(pocIndex);
        
        double vahPrice = minPrice + (vahIndex + 1) * priceStep;
        double valPrice = minPrice + valIndex * priceStep;
        
        // 결과 생성 (마지막 캔들 시간에 매물대 정보 반영)
        List<IndicatorResultDTO.IndicatorDataPoint> result = new ArrayList<>();
        
        // 가격대별 거래량 프로파일
        List<Map<String, Object>> profileData = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            double priceLevel = minPrice + (i + 0.5) * priceStep;
            profileData.add(Map.of(
                    "price", priceLevel,
                    "volume", volumeProfile.get(i).doubleValue(),
                    "isValueArea", valueArea.contains(i),
                    "isPOC", i == pocIndex
            ));
        }
        
        // 최종 결과 (마지막 캔들 시점)
        if (!candles.isEmpty()) {
            Map<String, Double> values = new HashMap<>();
            values.put("poc", pocPrice);
            values.put("vah", vahPrice);
            values.put("val", valPrice);
            values.put("minPrice", (double) minPrice);
            values.put("maxPrice", (double) maxPrice);
            
            result.add(IndicatorResultDTO.IndicatorDataPoint.builder()
                    .time(candles.get(candles.size() - 1).getTime())
                    .values(values)
                    .build());
        }
        
        log.debug("[VOLUME_PROFILE] Calculated: POC={}, VAH={}, VAL={}", pocPrice, vahPrice, valPrice);
        return result;
    }
    
    @Override
    public Map<String, Object> getDefaultParams() {
        return Map.of(
                "rows", DEFAULT_ROWS,
                "valueAreaPercentage", VALUE_AREA_PERCENTAGE
        );
    }
    
    @Override
    public boolean validateParams(Map<String, Object> params) {
        try {
            int rows = getIntParam(params, "rows", DEFAULT_ROWS);
            double valueAreaPct = getDoubleParam(params, "valueAreaPercentage", VALUE_AREA_PERCENTAGE);
            
            return rows > 0 && rows <= 100 && valueAreaPct > 0 && valueAreaPct <= 1.0;
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
