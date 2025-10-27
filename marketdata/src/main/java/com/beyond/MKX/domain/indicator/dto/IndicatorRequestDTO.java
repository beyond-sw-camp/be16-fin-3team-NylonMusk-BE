package com.beyond.MKX.domain.indicator.dto;

import com.beyond.MKX.domain.indicator.enums.IndicatorType;
import lombok.*;

import java.util.Map;

/**
 * 보조지표 계산 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorRequestDTO {
    
    private String ticker;              // 종목 코드
    private String interval;            // 캔들 간격 (1m, 5m, 15m, 30m, 1h, 4h, 1d)
    private IndicatorType indicatorType; // 지표 타입
    private Map<String, Object> params; // 지표별 파라미터 (예: MA의 period=20)
    
    /**
     * 이동평균선 요청 생성
     */
    public static IndicatorRequestDTO createMARequest(String ticker, String interval, int period) {
        return IndicatorRequestDTO.builder()
                .ticker(ticker)
                .interval(interval)
                .indicatorType(IndicatorType.MA)
                .params(Map.of("period", period))
                .build();
    }
    
    /**
     * RSI 요청 생성
     */
    public static IndicatorRequestDTO createRSIRequest(String ticker, String interval, int period) {
        return IndicatorRequestDTO.builder()
                .ticker(ticker)
                .interval(interval)
                .indicatorType(IndicatorType.RSI)
                .params(Map.of("period", period))
                .build();
    }
    
    /**
     * MACD 요청 생성
     */
    public static IndicatorRequestDTO createMACDRequest(String ticker, String interval, 
                                                         int fastPeriod, int slowPeriod, int signalPeriod) {
        return IndicatorRequestDTO.builder()
                .ticker(ticker)
                .interval(interval)
                .indicatorType(IndicatorType.MACD)
                .params(Map.of(
                    "fastPeriod", fastPeriod,
                    "slowPeriod", slowPeriod,
                    "signalPeriod", signalPeriod
                ))
                .build();
    }
    
    /**
     * 볼린저밴드 요청 생성
     */
    public static IndicatorRequestDTO createBollingerBandsRequest(String ticker, String interval, 
                                                                    int period, double stdDev) {
        return IndicatorRequestDTO.builder()
                .ticker(ticker)
                .interval(interval)
                .indicatorType(IndicatorType.BOLLINGER_BANDS)
                .params(Map.of(
                    "period", period,
                    "stdDev", stdDev
                ))
                .build();
    }
}
