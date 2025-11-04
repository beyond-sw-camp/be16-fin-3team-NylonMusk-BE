package com.beyond.MKX.domain.brokerage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 증권사 대시보드용 거래내역 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerDTO {
    private UUID id;
    private UUID orderLogId;
    private String ticker;
    private String stockName;
    private String transactionType; // BUY, SELL, DEPOSIT, WITHDRAWAL 등
    private String transactionTypeKorean;
    private Long quantity;
    private Long price;
    private Long totalAmount;
    private Long debit;
    private Long credit;
    private Long commission;
    private Long tax;
    private String accountNumber;
    private String memberName;
    private String counterpartyAccountNumber;
    private String counterpartyName;
    private LocalDateTime createdAt;
}

