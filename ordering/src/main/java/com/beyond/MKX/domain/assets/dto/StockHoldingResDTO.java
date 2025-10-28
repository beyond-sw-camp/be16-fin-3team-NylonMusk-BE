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
    private String accountNumber;  // 계좌번호 추가
    private UUID brokerageId;
    private String ticker;
    private String nameKo; // 기업명 필드 추가
    private Long totalQuantity;
    private Long availableQuantity;
    private Long totalPurchasePrice;

    public static StockHoldingResDTO from(StockHolding e, String accountNumber) {
        return StockHoldingResDTO.builder()
                .memberAccountId(e.getMemberAccountId())
                .accountNumber(accountNumber)
                .brokerageId(e.getBrokerageId())
                .ticker(e.getTicker())
                .nameKo(null) // StockHolding 엔티티에는 nameKo가 없으므로 null
                .totalQuantity(e.getTotalQuantity())
                .availableQuantity(e.getAvailableQuantity())
                .totalPurchasePrice(e.getTotalPurchasePrice())
                .build();
    }

    // 기존 메서드는 호환성을 위해 유지
    public static StockHoldingResDTO from(StockHolding e) {
        return from(e, null);
    }

}
