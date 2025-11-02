package com.beyond.MKX.domain.brokerage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 증권사 대시보드용 주문 내역 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLogDTO {
    private UUID id;
    private String ticker;
    private String stockName;
    private String orderKind; // MARKET, LIMIT, RESERVED
    private String side; // BUY, SELL
    private String status; // PENDING, FILLED, CANCELED 등
    private Long price;
    private Long quantity;
    private Long remainQuantity;
    private Long commission;
    private Long transactionTax;
    private String accountNumber;
    private String memberName;
    private LocalDateTime createdAt;
    private LocalDateTime filledAt;
    private LocalDateTime canceledAt;
}

