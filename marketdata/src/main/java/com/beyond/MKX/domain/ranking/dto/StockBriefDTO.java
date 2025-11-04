package com.beyond.MKX.domain.ranking.dto;

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
    private String imageUrl;
    private long totalSharesOutstanding; // 발행 주식 수 (시가총액 용)
}
