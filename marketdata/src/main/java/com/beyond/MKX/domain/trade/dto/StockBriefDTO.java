package com.beyond.MKX.domain.trade.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockBriefDTO {
    private UUID id;
    private String ticker;
    private String nameKo;
    private String status;
    private String delistingStage;
}
