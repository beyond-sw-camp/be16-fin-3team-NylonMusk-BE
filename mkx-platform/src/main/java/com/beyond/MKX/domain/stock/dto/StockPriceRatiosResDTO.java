package com.beyond.MKX.domain.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * STOCK_PRICE_RATIOS_RES_DTO: 현재가 기반 재무비율 응답 DTO
 * - 실시간 현재가 기반 비율 (PER, PBR, PSR, 시가총액, Enterprise Value)
 * - 1시간마다 업데이트 (매 정시)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPriceRatiosResDTO {
    private String ticker;
    private Long currentPrice;      // 현재가
    private Long marketCap;         // 시가총액
    private Long enterpriseValue;   // 기업 가치
    private BigDecimal per;         // PER (Price-to-Earnings Ratio)
    private BigDecimal pbr;         // PBR (Price-to-Book Ratio)
    private BigDecimal psr;         // PSR (Price-to-Sales Ratio)
}

