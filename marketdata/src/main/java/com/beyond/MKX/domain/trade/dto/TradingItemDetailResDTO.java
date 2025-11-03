package com.beyond.MKX.domain.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TradingItemDetailResDTO {
    private String ticker;
    private long currentPrice;
    private BigDecimal changeRate;
}
