package com.beyond.MKX.domain.tradingBot.dto;

import com.beyond.MKX.domain.tradingBot.entity.OrderKind;
import com.beyond.MKX.domain.tradingBot.entity.Side;
import jakarta.validation.constraints.*;

import java.util.UUID;

/**
 * 주문 요청 DTO (ordering 서비스와 동일한 구조)
 */
public record OrderRequestDTO(

        @NotNull(message = "주문 형식(MARKET, LIMIT, RESERVED)을 입력하세요.")
        OrderKind orderKind,

        @NotNull(message = "매수(BUY), 매도(SELL) 인지 입력하세요.")
        Side side,                // "BUY" | "SELL"

        UUID accountId,

        @NotBlank(message = "종목 코드는 필수입니다.")
        @Size(min = 6, max = 6, message = "종목 코드는 6자여야 합니다.")
        String ticker,

        // LIMIT만 사용. MARKET 이면 null
        Long price,

        @NotNull(message = "주문 수량을 입력하세요.")
        @Min(value = 1, message = "주문 수량은 1주 이상이어야 합니다.")
//        @Max(value = 100000, message = "10만 주를 초과할 수 없습니다.")
        Long quantity
) {
}

