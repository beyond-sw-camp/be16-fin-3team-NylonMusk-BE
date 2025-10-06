package com.beyond.MKX.domain.order.dto;

import com.beyond.MKX.domain.order.entity.OrderKind;
import com.beyond.MKX.domain.order.entity.Side;

import java.util.UUID;

public record OrderRequestDTO(
        OrderKind orderKind,
        Side side,                // "BUY" | "SELL"
        UUID accountId,
        String ticker,
        Long price,                 // LIMIT만 사용. MARKET 이면 null
        Long quantity
) {
}
