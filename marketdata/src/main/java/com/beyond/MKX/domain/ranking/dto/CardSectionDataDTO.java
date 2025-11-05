package com.beyond.MKX.domain.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카드섹션 데이터 DTO (Feign Client용)
 * 
 * mkx-platform에서 반환하는 인기/신규 종목 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSectionDataDTO {
    
    private List<StockBriefDTO> popular;  // 인기 종목 TOP N
    private List<StockBriefDTO> newest;   // 신규 종목 TOP N
}

