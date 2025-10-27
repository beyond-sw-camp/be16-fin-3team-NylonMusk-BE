package com.beyond.MKX.domain.indicator.dto;

import com.beyond.MKX.domain.indicator.enums.IndicatorType;
import lombok.*;

import java.util.Map;

/**
 * 사용자 지표 설정 DTO
 * 
 * 프론트엔드에서 사용자가 on/off한 지표 상태 관리
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserIndicatorConfigDTO {
    
    private String userId;                  // 사용자 ID (세션 ID 또는 실제 사용자 ID)
    private String ticker;                  // 종목 코드
    private String interval;                // 캔들 간격
    private IndicatorType indicatorType;    // 지표 타입
    private boolean enabled;                // on/off 상태
    private Map<String, Object> params;     // 지표 파라미터
    
    /**
     * Redis 저장용 키 생성
     */
    public String toRedisKey() {
        return String.format("indicator:config:%s:%s:%s:%s", 
                userId, ticker, interval, indicatorType.name());
    }
}
