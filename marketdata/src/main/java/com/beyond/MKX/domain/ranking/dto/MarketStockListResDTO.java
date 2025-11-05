package com.beyond.MKX.domain.ranking.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 마켓 종목 랭킹 리스트
 *
 * Redis Sorted Set(Z-Set)을 활용한 랭킹 데이터
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketStockListResDTO {
    private UUID id;               // <- 즐겨찾기 버튼 표시 조건
    private String ticker;
    private String name;           // nameKo
    private String status;         // 'SUSPENDED' 체크
    private String delistingStage; // 상장폐지 배지/거래불가 판단
    private String imageUrl;       // 종목 기업 이미지
    private long currentPrice;     // 현재가
    private BigDecimal changeRate; // 전일 대비 등락률 (%)
    private long tradingVolume;    // 거래량
    private long marketCap;        // 시가 총액
}
