package com.beyond.MKX.domain.stock.dto;

import com.beyond.MKX.domain.stock.entity.Stock;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record StockListResDto(
        UUID id,
        UUID corporationId,
        String ticker,
        String nameKo,
        String status,
        long totalSharesOutstanding,
        Long ownedShares,
        Long freeFloatShares,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public static StockListResDto from(Stock s) {
        return StockListResDto.builder()
                .id(s.getId())
                .corporationId(s.getCorporationId())
                .ticker(s.getTicker())
                .nameKo(s.getNameKo())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .totalSharesOutstanding(s.getTotalSharesOutstanding())
                .ownedShares(s.getOwnedShares())
                .freeFloatShares(s.getFreeFloatShares())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .deletedAt(s.getDeletedAt())
                .build();
    }
}
