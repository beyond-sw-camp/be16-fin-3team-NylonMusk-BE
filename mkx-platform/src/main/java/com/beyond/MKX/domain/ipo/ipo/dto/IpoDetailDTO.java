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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpoDetailDTO {
    private UUID ipoId;
    private UUID corporationId;
    private String corporationName;
    private String symbol;
    private IpoStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime listedAt;
    private String rejectReason;
    private Long faceValue;
    private Long preIpoOutstandingShares;
    private Boolean isOffering;
    private Long priceOnListing;
    
    // 파일 정보 (파일명만 표시)
    private String preShareholdersFileUrl;
    private String financialStatementsFileUrl;

    public static IpoDetailDTO of(Ipo i) {
        return IpoDetailDTO.builder()
                .ipoId(i.getId())
                .corporationId(i.getCorporation().getId())
                .corporationName(i.getCorporation().getNameKo())
                .symbol(i.getSymbol())
                .status(i.getStatus())
                .requestedAt(i.getRequestedAt())
                .reviewedAt(i.getReviewedAt())
                .listedAt(i.getListedAt())
                .rejectReason(i.getRejectReason())
                .faceValue(i.getFaceValue())
                .preIpoOutstandingShares(i.getPreIpoOutstandingShares())
                .isOffering(i.getIsOffering())
                .priceOnListing(i.getPriceOnListing())
                .preShareholdersFileUrl(i.getPreShareholdersFileUrl())
                .financialStatementsFileUrl(i.getFinancialStatementsUrl())
                .build();
    }
}
