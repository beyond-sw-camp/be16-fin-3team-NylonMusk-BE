package com.beyond.MKX.domain.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TradingHomeItemResDTO {
    private UUID id;               // <- 즐겨찾기 버튼 표시 조건
    private String ticker;
    private String name;           // nameKo
    private long currentPrice;
    private BigDecimal changeRate;
    private long turnover;
    private String status;         // 'SUSPENDED' 체크
    private String delistingStage; // 상장폐지 배지/거래불가 판단
    private String imageUrl;       // 종목 기업 이미지
}
