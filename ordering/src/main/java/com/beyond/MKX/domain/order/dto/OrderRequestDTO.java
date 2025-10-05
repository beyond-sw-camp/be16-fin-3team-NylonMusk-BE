package com.beyond.MKX.domain.order.dto;

import com.beyond.MKX.domain.order.entity.OrderKind;

import java.util.UUID;

public record OrderRequestDTO(
        OrderKind orderKind,
        String side,                // "BUY" | "SELL"
        UUID accountId,
        String ticker,
        Long price,                 // LIMIT만 사용. MARKET 이면 null
        Long quantity
) {
}
