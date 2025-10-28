package com.beyond.MKX.domain.execution.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Ledger extends BaseIdAndTimeEntity {

    @Column(nullable = false)
    private UUID orderLogId;

    @Column(nullable = false)
    private UUID creditAccountId;

    @Column(nullable = false)
    private UUID debitAccountId;

    @Column(nullable = false, columnDefinition = "VARCHAR(6)")
    private String ticker;

    // 정산일
    private LocalDateTime valuedAt;

    // 차변
    private Long debit;

    // 대변
    private Long credit;

    // 거래량
    @Column(nullable = false)
    private Long qtyChange;

    // 거래 가격
    @Column(nullable = false)
    private Long amountChange;

    @Column(nullable = false)
    private Long commission;

    private Long tax;

    // 거래 유형
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 20)
    private TransactionType transactionType;

}
