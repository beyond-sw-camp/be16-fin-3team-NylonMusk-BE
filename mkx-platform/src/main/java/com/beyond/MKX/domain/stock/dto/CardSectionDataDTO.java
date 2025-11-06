package com.beyond.MKX.domain.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카드섹션 데이터 DTO (내부 API용)
 * 
 * 인기 종목과 신규 종목을 한번에 조회하여 반환
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSectionDataDTO {
    
    private List<StockBriefDTO> popular;  // 인기 종목 TOP N
    private List<StockBriefDTO> newest;   // 신규 종목 TOP N
}

