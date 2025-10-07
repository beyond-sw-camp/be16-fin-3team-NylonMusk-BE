package com.beyond.MKX.domain.outbox.entity;

import com.beyond.MKX.domain.order.entity.OrderKind;
import com.beyond.MKX.domain.order.entity.Side;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderPayload {
    private UUID brokerageId;
    private UUID orderId;
    private String ticker;
    private Side side;
    private OrderKind orderKind;
    private Long price;
    private Long quantity;
    private LocalDateTime createdAt;
}
