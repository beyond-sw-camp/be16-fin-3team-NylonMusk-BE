package com.beyond.MKX.domain.ipo.bookbuilding.dto;

import com.beyond.MKX.domain.ipo.bookbuilding.entity.IpoBookBuilding;
import com.beyond.MKX.domain.ipo.bookbuilding.entity.ParticipantType;
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
public class IpoBookBuildingResDTO {
    private UUID ipoBookBuildingId;
    private UUID ipoOfferingId;
    private ParticipantType participantType;
    private UUID participantId;           // 참여 주체 (Corporation or Brokerage)
    private Long bidPrice;                // 희망가격
    private Long bidQuantity;             // 희망수량
    private Boolean acceptAllPrices;      // 모든 가격 구간 참여 여부

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static IpoBookBuildingResDTO from(IpoBookBuilding ipoBookBuilding) {
        return IpoBookBuildingResDTO.builder()
                .ipoBookBuildingId(ipoBookBuilding.getId())
                .ipoOfferingId(ipoBookBuilding.getIpoOffering().getId())
                .participantType(ipoBookBuilding.getParticipantType())
                .participantId(ipoBookBuilding.getParticipantId())
                .bidPrice(ipoBookBuilding.getBidPrice())
                .bidQuantity(ipoBookBuilding.getBidQuantity())
                .acceptAllPrices(ipoBookBuilding.getAcceptAllPrices())
                .build();
    }
}
