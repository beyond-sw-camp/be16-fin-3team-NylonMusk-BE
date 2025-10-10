package com.beyond.MKX.infrastructure.kafka.event;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class InboundOrderMessage {
    private String brokerageId;
    private String orderId;
    private String ticker;     // 예: "005930"
    private String side;       // "BUY" / "SELL"
    private String orderKind;  // "LIMIT" / "MARKET"
    private Long   price;      // 원화 정수. LIMIT일 때만 사용
    private Double quantity;
    private String createdAt;  // 문자열이면 일단 그대로 둠
}