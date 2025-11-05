package com.beyond.MKX.domain.execution.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
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
@Table(
        name = "ledger",
        indexes = {
                // 단일 인덱스
                @Index(name = "idx_order_log_id", columnList = "orderLogId"),
                @Index(name = "idx_credit_account_id", columnList = "creditAccountId"),
                @Index(name = "idx_debit_account_id", columnList = "debitAccountId"),
                @Index(name = "idx_ticker", columnList = "ticker"),
                @Index(name = "idx_transaction_type", columnList = "transaction_type"),
                @Index(name = "idx_valued_at", columnList = "valuedAt"),
                // 복합 인덱스
                @Index(name = "idx_account_transaction_date", columnList = "creditAccountId, transaction_type, valuedAt"),
                @Index(name = "idx_ticker_date", columnList = "ticker, valuedAt")
        }
)
public class Ledger extends BaseIdAndTimeEntity {

    private UUID orderLogId;

    private UUID creditAccountId;

    private UUID debitAccountId;

    @Column(nullable = true, columnDefinition = "VARCHAR(6)")
    private String ticker;

    // 정산일
    private LocalDateTime valuedAt;

    // 차변
    private Long debit;

    // 대변
    private Long credit;

    // 거래량
    private Long qtyChange;

    // 거래 가격
    private Long amountChange;

    private Long commission;

    private Long tax;

    // 거래 유형
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 20)
    private TransactionType transactionType;

    // 계좌이체 시 상대방 계좌번호
    @Column(name = "counterparty_account_number", length = 50)
    private String counterpartyAccountNumber;

    // 계좌이체 시 상대방 이름 (회원명 또는 기업명)
    @Column(name = "counterparty_name", length = 100)
    private String counterpartyName;

}
