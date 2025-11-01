package com.beyond.MKX.domain.stock.dto;

import com.beyond.MKX.domain.delisting.entity.DelistingStage;
import com.beyond.MKX.domain.stock.entity.Stock;
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
    private Stock.Status status;
    private DelistingStage delistingStage;
}
