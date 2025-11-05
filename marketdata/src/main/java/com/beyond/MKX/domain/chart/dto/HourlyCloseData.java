package com.beyond.MKX.domain.chart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 시간별 종가 데이터
 * 
 * 미니차트에서 사용하는 1시간 단위 종가 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HourlyCloseData {
    
    private Instant timestamp;  // 시간
    private long close;         // 종가
}

