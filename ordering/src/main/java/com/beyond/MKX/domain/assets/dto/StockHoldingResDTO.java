package com.beyond.MKX.domain.assets.dto;

import com.beyond.MKX.domain.assets.entity.OwnerType;
import com.beyond.MKX.domain.assets.entity.StockHolding;
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
public class StockHoldingResDTO {
    private UUID memberAccountId;
    private UUID brokerageId;
    private OwnerType ownerType;
    private String ticker;
    private Long totalQuantity;
    private Long totalPurchasePrice;

    public static StockHoldingResDTO from(StockHolding e) {
        return StockHoldingResDTO.builder()
                .memberAccountId(e.getMemberAccountId())
                .brokerageId(e.getBrokerageId())
                .ownerType(e.getOwnerType())
                .ticker(e.getTicker())
                .totalQuantity(e.getTotalQuantity())
                .totalPurchasePrice(e.getTotalPurchasePrice())
                .build();
    }

}
