package com.beyond.MKX.domain.ipo.offering.dto;

import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IpoOfferingResDTO {
    private UUID id;
    private UUID ipoId;
    private Integer roundNo;
    private Long offerQuantity;
    private Long lotSize;
    private Long priceBandMin;
    private Long priceBandMax;
    private Long offerPrice; // 확정 전이면 null
    private java.math.BigDecimal depositRate;
    private java.math.BigDecimal competitionRatio;
    private java.time.LocalDateTime subscriptionStart;
    private java.time.LocalDateTime subscriptionEnd;
    private java.time.LocalDate allocationDate;
    private java.time.LocalDate refundDate;
    private IpoOfferingStatus status;

    public static IpoOfferingResDTO from(IpoOffering o) {
        return IpoOfferingResDTO.builder()
                .id(o.getId())
                .ipoId(o.getIpo().getId())
                .roundNo(o.getRoundNo())
                .offerQuantity(o.getOfferQuantity())
                .lotSize(o.getLotSize())
                .priceBandMin(o.getPriceBandMin())
                .priceBandMax(o.getPriceBandMax())
                .offerPrice(o.getOfferPrice())
                .depositRate(o.getDepositRate())
                .competitionRatio(o.getCompetitionRatio())
                .subscriptionStart(o.getSubscriptionStart())
                .subscriptionEnd(o.getSubscriptionEnd())
                .allocationDate(o.getAllocationDate())
                .refundDate(o.getRefundDate())
                .status(o.getIpoOfferingStatus())
                .build();
    }
}

