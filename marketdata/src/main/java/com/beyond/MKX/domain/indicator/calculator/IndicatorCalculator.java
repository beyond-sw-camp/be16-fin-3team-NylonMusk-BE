package com.beyond.MKX.domain.indicator.calculator;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;

import java.util.List;
import java.util.Map;

/**
 * 보조지표 계산 인터페이스
 * 
 * 모든 지표 계산기는 이 인터페이스를 구현
 */
public interface IndicatorCalculator {
    
    /**
     * 지표 계산
     * 
     * @param candles 캔들 데이터 (시간순 정렬)
     * @param params 지표별 파라미터
     * @return 계산된 지표 데이터 포인트 리스트
     */
    List<IndicatorResultDTO.IndicatorDataPoint> calculate(List<Candle> candles, Map<String, Object> params);
    
    /**
     * 기본 파라미터 반환
     */
    Map<String, Object> getDefaultParams();
    
    /**
     * 파라미터 유효성 검증
     */
    boolean validateParams(Map<String, Object> params);
}
