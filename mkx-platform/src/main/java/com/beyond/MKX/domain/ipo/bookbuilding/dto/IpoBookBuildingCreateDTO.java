package com.beyond.MKX.domain.ipo.bookbuilding.dto;

import com.beyond.MKX.domain.ipo.bookbuilding.entity.ParticipantType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpoBookBuildingCreateDTO {
    private UUID ipoOfferingId;           // 공모 ID
    private ParticipantType participantType; // CORPORATION, BROKERAGE, EXCHANGE
    private UUID participantId;           // 기관/증권사 ID

    private Long bidPrice;                // 희망가격 (nullable)
    private Long bidQuantity;             // 신청수량
}
