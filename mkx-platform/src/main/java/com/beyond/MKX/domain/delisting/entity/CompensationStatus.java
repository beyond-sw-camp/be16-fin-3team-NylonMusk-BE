package com.beyond.MKX.domain.delisting.entity;

import lombok.Getter;

/**
 * 상장폐지 보상 처리 상태 Enum
 * 
 * 상장폐지 보상금의 처리 과정을 단계별로 관리합니다.
 * 금융 거래의 투명성과 추적성을 보장하기 위한 상태 관리입니다.
 */
@Getter
public enum CompensationStatus {
    
    /**
     * 보상 대기
     * 상장폐지 예고 시 보상 요청이 생성된 상태
     */
    PENDING("대기"),
    
    /**
     * 보상 처리중
     * 보상금 지급 절차가 진행 중인 상태
     */
    PROCESSING("처리중"),
    
    /**
     * 보상 완료
     * 보상금이 성공적으로 지급된 상태
     */
    COMPLETED("완료"),
    
    /**
     * 보상 부분 지급
     * 보상금이 일부만 지급된 상태 (유동자산 부족 등)
     */
    PARTIAL_PAID("부분지급"),
    
    /**
     * 보상 실패
     * 보상금 지급에 실패한 상태
     */
    FAILED("실패"),
    
    /**
     * 보상 취소
     * 보상 요청이 취소된 상태
     */
    CANCELLED("취소");

    private final String description;

    CompensationStatus(String description) {
        this.description = description;
    }
}
