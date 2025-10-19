package com.beyond.MKX.domain.tradingBot.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "trading_bot_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingBotConfig extends BaseIdAndTimeEntity {
    
    @Column(name = "ticker", nullable = false, length = 10)
    private String ticker;           // 종목 코드
    
    @Column(name = "status", nullable = false, length = 10)
    private String status;            // START/END
    
    @Column(name = "price_limit_high")
    private Long priceLimitHigh;      // 상한가
    
    @Column(name = "price_limit_low")
    private Long priceLimitLow;       // 하한가
    
    @Column(name = "quantity", precision = 19, scale = 0)
    private BigDecimal quantity;      // 주문 수량
    
    @Column(name = "side", nullable = false, length = 10)
    private String side;              // BUY/SELL
    
    @Column(name = "order_type", nullable = false, length = 10)
    private String orderType;         // LIMIT/MARKET
    
    @Column(name = "brokerage_id", nullable = false, length = 50)
    private String brokerageId;       // 증권사 ID
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;  // 활성화 여부
    
    @Column(name = "description", length = 500)
    private String description;       // 설명
}
