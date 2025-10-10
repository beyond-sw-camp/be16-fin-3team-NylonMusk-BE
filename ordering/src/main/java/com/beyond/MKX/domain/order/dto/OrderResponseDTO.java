package com.beyond.MKX.domain.order.dto;

import com.beyond.MKX.domain.order.entity.OrderLog;
import com.beyond.MKX.domain.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderResponseDTO {
    private UUID orderId;
    private OrderStatus orderStatus;

    public static OrderResponseDTO from(OrderLog order) {
        return OrderResponseDTO.builder().orderId(order.getId()).orderStatus(order.getStatus()).build();
    }
}
