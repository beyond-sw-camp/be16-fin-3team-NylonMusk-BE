package com.beyond.MKX.domain.ranking.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 거래대금/거래량 랭킹 DTO
 *
 * Redis Sorted Set(Z-Set)을 활용한 랭킹 데이터
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingRankDTO {

    /**
     * 종목 코드
     */
    private String ticker;

    /**
     * 종목명 (선택)
     */
    private String tickerName;

    /**
     * 거래대금 (24시간)
     * = sum(price * quantity)
     */
    private BigDecimal tradingValue;

    /**
     * 거래량 (24시간)
     * = sum(quantity)
     */
    private BigDecimal tradingVolume;

    /**
     * 거래대금 랭킹 순위
     */
    private Integer valueRank;

    /**
     * 거래량 랭킹 순위
     */
    private Integer volumeRank;

    /**
     * 현재가 (참고용)
     */
    private Long currentPrice;

    /**
     * 전일 대비 등락률 (%)
     */
    private Double changeRate;

    /**
     * 데이터 갱신 시각
     */
    private Instant updatedAt;

    /**
     * 24시간 시작 시각
     */
    private Instant startTime;

    /**
     * 24시간 종료 시각
     */
    private Instant endTime;
}
