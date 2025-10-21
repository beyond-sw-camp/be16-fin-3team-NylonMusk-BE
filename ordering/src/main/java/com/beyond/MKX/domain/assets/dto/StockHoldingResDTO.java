package com.beyond.MKX.domain.assets.dto;

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
    private String ticker;
    private Long totalQuantity;
    private Long availableQuantity;
    private Long totalPurchasePrice;

    public static StockHoldingResDTO from(StockHolding e) {
        return StockHoldingResDTO.builder()
                .memberAccountId(e.getMemberAccountId())
                .brokerageId(e.getBrokerageId())
                .ticker(e.getTicker())
                .totalQuantity(e.getTotalQuantity())
                .availableQuantity(e.getAvailableQuantity())
                .totalPurchasePrice(e.getTotalPurchasePrice())
                .build();
    }

}
