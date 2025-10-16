package com.beyond.MKX.domain.ipo.ipo.dto;

import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.ipo.entity.IpoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IpoListResDTO {
    private UUID ipoId;
    private UUID corporationId;

    private String symbol;
    private IpoStatus status;

    private LocalDateTime listingAt;
    private Long priceOnListing;

    // 스냅샷
    private Long preIpoOutstandingShares;
    private Long outstandingSharesAtListing;   // = pre + issued(공모 정산 완료분)
    private Double lockupRatio;
    private LocalDateTime lockupExpiryAt;

    // 공모 여부/스냅샷(옵션)
    private Boolean isOffering;
    private Long issuedQuantity;               // 마지막 라운드의 issuedQuantity(SETTLED 기준)

    // 생성된 종목 정보
    private String ticker;
    private String nameKo;

    public static IpoListResDTO of(Ipo ipo, String ticker, Long issuedQuantity) {
        return IpoListResDTO.builder()
                .ipoId(ipo.getId())
                .corporationId(ipo.getCorporation().getId())
                .symbol(ipo.getSymbol())
                .status(ipo.getStatus())
                .listingAt(ipo.getListingAt())
                .priceOnListing(ipo.getPriceOnListing())
                .preIpoOutstandingShares(ipo.getPreIpoOutstandingShares())
                .outstandingSharesAtListing(ipo.getOutstandingSharesAtListing())
                .lockupRatio(ipo.getLockupRatio())
                .lockupExpiryAt(ipo.getLockupExpiryAt())
                .isOffering(ipo.getIsOffering())
                .issuedQuantity(issuedQuantity)
                .ticker(ticker)
                .nameKo(ipo.getSymbol())
                .build();
    }
}
