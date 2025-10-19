package com.beyond.MKX.domain.ipo.allocation.dto;

import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockUpdateDTO {

    // 멱등키: allocationId를 그대로 써도 됩니다.
    @NotNull
    private UUID idempotencyKey;

    @NotNull
    private UUID allocationId;   // 추적/멱등 겸용

    private UUID offeringId;     // (추적용)

    // 대상
    @NotNull
    private UUID memberAccountId;
    @NotNull
    private String ticker;       // 6자리 등

    // 변경량
    @NotNull
    private Long qtyDelta;       // +배정 / -환수

    // 옵션(총취득가 반영용)
    private Long unitPrice;

    @NotNull
    private String reason;       // "IPO_ALLOCATION" 등

}
