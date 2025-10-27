package com.beyond.MKX.domain.ipo.bookbuilding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpoBookBuildingAvailableResDTO {
    private UUID ipoOfferingId;
    private UUID ipoId;
    private String ipoSymbol;
    private String corporationName;
    private Long offerQuantity;
    private Integer lotSize;
    private Long priceBandMin;
    private Long priceBandMax;
    private BigDecimal depositRate;
    private String status;
    private Boolean alreadyParticipated;
}

