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
public class OrderResponseDTO {
    private UUID orderId;
    private OrderStatus orderStatus;
    private String ticker;
    private Side side;
    private Long price;
    private Long quantity;
    private Long remainQuantity;
    private OrderKind orderKind;
    private LocalDateTime createdAt;

    public static OrderResponseDTO from(OrderLog order) {
        return OrderResponseDTO.builder()
                .orderId(order.getId())
                .orderStatus(order.getStatus())
                .ticker(order.getTicker())
                .side(order.getSide())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .remainQuantity(order.getRemainQuantity())
                .orderKind(order.getOrderKind())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
