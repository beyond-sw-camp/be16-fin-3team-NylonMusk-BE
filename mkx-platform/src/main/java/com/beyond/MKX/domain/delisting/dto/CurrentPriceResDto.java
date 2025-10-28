package com.beyond.MKX.domain.delisting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * marketdata 서비스의 CurrentPrice와 매핑되는 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record CurrentPriceResDto(
        String ticker,              // 종목 코드
        Long price,                 // 현재가 (최신 체결가)
        Long prevClose,             // 전일 종가
        Long open,                  // 시가 (당일 첫 체결가)
        Long high,                  // 고가 (당일 최고 체결가)
        Long low,                   // 저가 (당일 최저 체결가)
        Long change,                // 전일대비 등락액
        BigDecimal changeRate,      // 전일대비 등락률 (%)
        BigDecimal volume,          // 당일 누적 거래량
        Long bestBid,               // 최우선 매수호가
        BigDecimal bestBidQuantity, // 최우선 매수호가 수량
        Long bestAsk,               // 최우선 매도호가
        BigDecimal bestAskQuantity, // 최우선 매도호가 수량
        Long spread,                // 스프레드
        Instant timestamp           // 최종 업데이트 시각
) {}
