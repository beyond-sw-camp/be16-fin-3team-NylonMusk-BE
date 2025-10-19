package com.beyond.MKX.domain.tradingBot.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTradingBotConfigRequest {
    
    private String ticker;           // 종목 코드
    private String status;           // START/END
    private Long priceLimitHigh;     // 상한가
    private Long priceLimitLow;      // 하한가
    private BigDecimal quantity;     // 주문 수량
    private String side;             // BUY/SELL
    private String orderType;        // LIMIT/MARKET
    private String brokerageId;      // 증권사 ID
    private String description;      // 설명
}
