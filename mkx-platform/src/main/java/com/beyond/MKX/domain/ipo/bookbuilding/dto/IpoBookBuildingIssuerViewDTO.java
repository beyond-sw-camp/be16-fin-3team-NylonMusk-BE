package com.beyond.MKX.domain.ipo.bookbuilding.dto;

import com.beyond.MKX.domain.ipo.bookbuilding.entity.IpoBookBuilding;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpoBookBuildingIssuerViewDTO {
    private String participantName;  // 기업명
    private UUID participantId;
    private Long bidPrice;
    private Long bidQuantity;
    private Boolean acceptAllPrices;
    private LocalDateTime createdAt;

    public static IpoBookBuildingIssuerViewDTO from(IpoBookBuilding bookBuilding, String participantName) {
        return IpoBookBuildingIssuerViewDTO.builder()
                .participantName(participantName)
                .participantId(bookBuilding.getParticipantId())
                .bidPrice(bookBuilding.getBidPrice())
                .bidQuantity(bookBuilding.getBidQuantity())
                .acceptAllPrices(bookBuilding.getAcceptAllPrices())
                .createdAt(bookBuilding.getCreatedAt())
                .build();
    }
}
