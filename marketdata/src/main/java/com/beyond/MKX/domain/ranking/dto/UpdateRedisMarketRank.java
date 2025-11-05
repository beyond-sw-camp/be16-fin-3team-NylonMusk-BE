package com.beyond.MKX.domain.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRedisMarketRank {
    private String ticker;
    private long tradeValue;
    private long volume;
    private LocalDate tradingDate;
}
