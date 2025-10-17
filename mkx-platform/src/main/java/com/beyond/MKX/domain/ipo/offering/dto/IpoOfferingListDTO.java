package com.beyond.MKX.domain.ipo.offering.dto;

import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpoOfferingListDTO {
    private UUID offeringId;
    private Integer roundNo;
    private String ipoSymbol;     // 상세에서 굳이 또 조회 안 하게
    private String ipoNameKo;
    private IpoOfferingStatus status;
    private Long priceBandMin;
    private Long priceBandMax;
    private Long offerPrice;      // PRICE_FIXED면 노출
    private Long offerQuantity;
    private Integer lotSize;
    private LocalDateTime subscriptionStart;
    private LocalDateTime subscriptionEnd;
    private boolean subscribable; // now ∈ [start,end)
    private BigDecimal competitionRatio;

    public static IpoOfferingListDTO from(IpoOffering e, Clock clock) {
        var now = LocalDateTime.now(clock);
        boolean sub = e.getIpoOfferingStatus()==IpoOfferingStatus.OPEN
                && !now.isBefore(e.getSubscriptionStart())
                &&  now.isBefore(e.getSubscriptionEnd());
        return IpoOfferingListDTO.builder()
                .offeringId(e.getId())
                .roundNo(e.getRoundNo())
                .ipoSymbol(e.getIpo().getSymbol())
                .ipoNameKo(e.getIpo().getCorporation().getNameKo())
                .status(e.getIpoOfferingStatus())
                .priceBandMin(e.getPriceBandMin())
                .priceBandMax(e.getPriceBandMax())
                .offerPrice(e.getOfferPrice())
                .offerQuantity(e.getOfferQuantity())
                .lotSize(e.getLotSize())
                .subscriptionStart(e.getSubscriptionStart())
                .subscriptionEnd(e.getSubscriptionEnd())
                .subscribable(sub)
                .competitionRatio(e.getCompetitionRatio())
                .build();
    }
}
