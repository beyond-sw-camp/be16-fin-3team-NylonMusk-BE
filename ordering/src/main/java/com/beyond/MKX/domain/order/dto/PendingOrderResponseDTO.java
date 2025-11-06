package com.beyond.MKX.domain.order.dto;

import com.beyond.MKX.domain.order.entity.OrderKind;
import com.beyond.MKX.domain.order.entity.OrderLog;
import com.beyond.MKX.domain.order.entity.OrderStatus;
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
public class PendingOrderResponseDTO {
    private UUID orderId;
    private String ticker;
    private Side side;
    private Long price;
    private Long quantity;
    private Long remainQuantity;
    private OrderKind orderKind;
    private OrderStatus status;
    private LocalDateTime createdAt;

    public static PendingOrderResponseDTO from(OrderLog order) {
        return PendingOrderResponseDTO.builder()
                .orderId(order.getId())
                .ticker(order.getTicker())
                .side(order.getSide())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .remainQuantity(order.getRemainQuantity())
                .orderKind(order.getOrderKind())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}

