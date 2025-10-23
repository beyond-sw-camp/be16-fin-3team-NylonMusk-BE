package com.beyond.MKX.domain.delisting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.UUID;

/**
 * ordering 서비스의 StockHoldingResDTO와 매핑되는 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record StockHoldingResDto(
        UUID memberAccountId,
        UUID brokerageId,
        String ticker,
        Long totalQuantity,
        Long availableQuantity,
        Long totalPurchasePrice
) {}
