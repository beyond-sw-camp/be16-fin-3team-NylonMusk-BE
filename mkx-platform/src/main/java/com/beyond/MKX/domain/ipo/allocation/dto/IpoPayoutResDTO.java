package com.beyond.MKX.domain.ipo.allocation.dto;

import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigInteger;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IpoPayoutResDTO {
    private String offeringId;
    private Long issuedQuantity;
    private Long offerPrice;
    private BigInteger totalProceeds;
    private String newStatus;

    public static IpoPayoutResDTO of(IpoOffering o, BigInteger totalProceeds) {
        return IpoPayoutResDTO.builder()
                .offeringId(o.getId().toString())
                .issuedQuantity(o.getIssuedQuantity())
                .offerPrice(o.getOfferPrice())
                .totalProceeds(totalProceeds)
                .newStatus(o.getIpoOfferingStatus().name())
                .build();
    }
}
