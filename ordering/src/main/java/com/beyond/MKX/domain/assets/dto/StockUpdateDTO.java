package com.beyond.MKX.domain.assets.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockUpdateDTO {
    @NotNull private UUID idempotencyKey; // 보통 allocationId
    @NotNull private UUID allocationId;

    private UUID offeringId;              // 선택(추적용)

    @NotNull private UUID memberAccountId; // 주의: 계좌ID를 넣어 받기로 합의
    private UUID brokerageId;
    @NotNull private String ticker;

    @NotNull private Long qtyDelta;       // +배정 / -환수
    private Long unitPrice;               // 선택
    @NotNull
    private String reason;       // "IPO_ALLOCATION"
}
