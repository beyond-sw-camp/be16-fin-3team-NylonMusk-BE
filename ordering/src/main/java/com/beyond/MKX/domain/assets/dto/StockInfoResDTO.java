package com.beyond.MKX.domain.assets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInfoResDTO {
    private String ticker;
    private String nameKo;
}
