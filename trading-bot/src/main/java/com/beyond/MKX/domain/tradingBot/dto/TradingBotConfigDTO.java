package com.beyond.MKX.domain.tradingBot.dto;

import com.beyond.MKX.domain.tradingBot.entity.BotExecutionStatus;
import com.beyond.MKX.domain.tradingBot.entity.TradingStrategy;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingBotConfigDTO {
    
    private UUID id;
    private String ticker;           // 종목 코드
    private String status;            // START/END
    private Long priceLimitHigh;     // 상한가
    private Long priceLimitLow;      // 하한가
    private BigDecimal quantity;     // 주문 수량
    private String side;             // BUY/SELL
    private String orderType;        // LIMIT/MARKET
    private String brokerageId;      // 증권사 ID
    private Boolean isActive;        // 활성화 여부
    private String description;      // 설명
    
    // 새로운 필드들
    private UUID buyAccountId;       // 매수 계좌 ID
    private UUID sellAccountId;      // 매도 계좌 ID
    private TradingStrategy tradingStrategy;  // 트렌드 전략
    
    // 상태 정보
    private BotExecutionStatus lastExecutionStatus;  // 마지막 실행 상태
    private String lastErrorMessage;  // 마지막 에러 메시지
    private LocalDateTime lastExecutionTime;  // 마지막 실행 시간
    private Integer consecutiveSkipCount;  // 연속 스킵 횟수
    private Long totalOrderCount;  // 총 주문 성공 횟수
    private Long totalSkipCount;  // 총 스킵 횟수
}
