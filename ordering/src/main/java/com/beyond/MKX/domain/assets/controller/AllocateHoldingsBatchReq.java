package com.beyond.MKX.domain.assets.controller;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
public record AllocateHoldingsBatchReq(
        @NotNull List<Item> items
) {
    public record Item(
            @NotNull UUID allocationEventId,   // 멱등키(권장: IpoAllocation.id)
            @NotNull UUID memberAccountId,
            @NotNull UUID brokerageId,
            @NotBlank String ticker,           // 상장 티커 (VARCHAR(6))
            @Positive long quantity,           // 배정 수량
            @Positive long pricePerShare,      // 확정 공모가(취득단가)
            @Min(0) long lockedQuantity        // 락업 수량(available 제외)
    ) {
    }
}
