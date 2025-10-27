package com.beyond.MKX.domain.indicator.dto;

import com.beyond.MKX.domain.indicator.enums.IndicatorType;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 보조지표 계산 결과 DTO
 * 
 * 프론트엔드로 전송되는 지표 데이터 구조
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class IndicatorResultDTO {
    
    private String ticker;                  // 종목 코드
    private String interval;                // 캔들 간격
    private IndicatorType indicatorType;    // 지표 타입
    private Map<String, Object> params;     // 사용된 파라미터
    
    // 지표 데이터 (시계열)
    private List<IndicatorDataPoint> data;
    
    // 메타데이터
    private Instant calculatedAt;           // 계산 시각
    private int dataPointCount;             // 데이터 포인트 개수
    
    /**
     * 개별 지표 데이터 포인트
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class IndicatorDataPoint {
        private Instant time;                   // 시각
        private Map<String, Double> values;     // 지표 값들 (예: MA는 {"ma": 50000.0}, MACD는 {"macd": 100.0, "signal": 80.0, "histogram": 20.0})
    }
    
    /**
     * 단일 값 지표 결과 생성 (MA, EMA 등)
     */
    public static IndicatorResultDTO createSingleValueResult(
            String ticker, String interval, IndicatorType indicatorType,
            Map<String, Object> params, List<IndicatorDataPoint> data) {
        
        return IndicatorResultDTO.builder()
                .ticker(ticker)
                .interval(interval)
                .indicatorType(indicatorType)
                .params(params)
                .data(data)
                .calculatedAt(Instant.now())
                .dataPointCount(data.size())
                .build();
    }
    
    /**
     * 복수 값 지표 결과 생성 (MACD, 볼린저밴드 등)
     */
    public static IndicatorResultDTO createMultiValueResult(
            String ticker, String interval, IndicatorType indicatorType,
            Map<String, Object> params, List<IndicatorDataPoint> data) {
        
        return IndicatorResultDTO.builder()
                .ticker(ticker)
                .interval(interval)
                .indicatorType(indicatorType)
                .params(params)
                .data(data)
                .calculatedAt(Instant.now())
                .dataPointCount(data.size())
                .build();
    }
}
