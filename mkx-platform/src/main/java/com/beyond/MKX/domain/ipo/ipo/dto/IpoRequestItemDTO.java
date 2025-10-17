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
public class IpoRequestItemDTO {
    private UUID ipoId;
    private UUID corporationId;
    private String corporationName;
    private String symbol;
    private IpoStatus status;
    private LocalDateTime requestedAt;

    public static IpoRequestItemDTO of(Ipo i) {
        return IpoRequestItemDTO.builder()
                .ipoId(i.getId())
                .corporationId(i.getCorporation().getId())
                .corporationName(i.getCorporation().getNameKo())
                .symbol(i.getSymbol())
                .status(i.getStatus())
                .requestedAt(i.getRequestedAt())
                .build();
    }
}
