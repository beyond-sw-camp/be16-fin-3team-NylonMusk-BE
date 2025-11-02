package com.beyond.MKX.domain.brokerage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 증권사별 인기 종목 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularStockDTO {
    private String ticker;
    private String stockName; // 종목명 (한글)
    private Long customerCount; // 보유 고객 수 (distinct memberAccountId 개수)
    private Long totalQuantity; // 총 보유량 (totalQuantity 합계)
}
