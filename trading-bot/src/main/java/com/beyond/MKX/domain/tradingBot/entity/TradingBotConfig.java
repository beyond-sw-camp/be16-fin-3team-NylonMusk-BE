package com.beyond.MKX.domain.tradingBot.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
    
    @Column(name = "side", length = 10)
    private String side;              // BUY/SELL (deprecated - buyAccountId/sellAccountId 사용)
    
    @Column(name = "order_type", nullable = false, length = 10)
    private String orderType;         // LIMIT/MARKET
    
    @Column(name = "brokerage_id", nullable = false, length = 50)
    private String brokerageId;       // 증권사 ID
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;  // 활성화 여부
    
    @Column(name = "description", length = 500)
    private String description;       // 설명
    
    // 새로운 필드들
    @Column(name = "buy_account_id")
    private UUID buyAccountId;        // 매수 계좌 ID
    
    @Column(name = "sell_account_id")
    private UUID sellAccountId;       // 매도 계좌 ID
    
    @Enumerated(EnumType.STRING)
    @Column(name = "trading_strategy", length = 20)
    private TradingStrategy tradingStrategy;  // 트렌드 전략
    
    // 상태 추적 필드들
    @Enumerated(EnumType.STRING)
    @Column(name = "last_execution_status", length = 30)
    private BotExecutionStatus lastExecutionStatus;  // 마지막 실행 상태
    
    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;  // 마지막 에러 메시지
    
    @Column(name = "last_execution_time")
    private LocalDateTime lastExecutionTime;  // 마지막 실행 시간
    
    @Column(name = "consecutive_skip_count")
    @Builder.Default
    private Integer consecutiveSkipCount = 0;  // 연속 스킵 횟수
    
    @Column(name = "total_order_count")
    @Builder.Default
    private Long totalOrderCount = 0L;  // 총 주문 성공 횟수
    
    @Column(name = "total_skip_count")
    @Builder.Default
    private Long totalSkipCount = 0L;  // 총 스킵 횟수
}
