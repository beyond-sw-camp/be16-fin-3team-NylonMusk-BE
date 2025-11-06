package com.beyond.MKX.domain.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 카드섹션 아이템 DTO
 * 
 * 각 카드섹션(인기/신규/상승률/거래량)에 표시될 종목 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSectionItemDTO {
    private UUID id;               // 종목 ID
    private String ticker;         // 종목 코드
    private String nameKo;         // 종목명
    private long currentPrice;     // 현재가
    private BigDecimal changeRate; // 등락률 (%)
}

