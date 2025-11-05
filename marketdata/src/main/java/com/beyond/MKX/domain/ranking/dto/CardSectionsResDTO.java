package com.beyond.MKX.domain.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카드섹션 응답 DTO
 * 
 * 4개 카드섹션(인기/신규/상승률/거래량) 각각 TOP 3 종목 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSectionsResDTO {
    private List<CardSectionItemDTO> popular;       // 인기 TOP 3
    private List<CardSectionItemDTO> newest;        // 신규 TOP 3
    private List<CardSectionItemDTO> topChangeRate; // 상승률 TOP 3
    private List<CardSectionItemDTO> topVolume;     // 거래량 TOP 3
}

