package com.beyond.MKX.domain.stockfavorite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockFavoritesResDTO {
    private UUID stockId;
    private String nameKo;
    private String ticker;
}
