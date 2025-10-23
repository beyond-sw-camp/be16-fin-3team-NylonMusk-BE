package com.beyond.MKX.domain.order.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderCancelRequestDTO(
        @NotNull(message = "orderLogId가 null입니다.")
        UUID orderLogId
) {
}
