package com.beyond.MKX.domain.chart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 미니차트 응답 DTO
 * 
 * 특정 종목의 24시간 시간별 종가 데이터 (최대 24개)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiniChartResDTO {
    
    private String ticker;                      // 종목 코드
    private List<HourlyCloseData> hourlyCloses; // 24개의 시간별 종가
}

