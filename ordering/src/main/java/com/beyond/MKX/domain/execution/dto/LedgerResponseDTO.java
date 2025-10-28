package com.beyond.MKX.domain.execution.dto;

import com.beyond.MKX.domain.execution.entity.Ledger;
import com.beyond.MKX.domain.execution.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerResponseDTO {
    private UUID id;
    private String ticker;
    private String stockName;
    private TransactionType transactionType;
    private String transactionTypeKorean;
    private Long quantity;
    private Long price;
    private Long totalAmount;
    private Long commission;
    private Long tax;
    private Long netAmount;
    private LocalDateTime transactionDate;

    /**
     * Ledger 엔티티를 LedgerResponseDTO로 변환합니다.
     * 종목명(stockName)은 별도로 Feign을 통해 조회해야 합니다.
     */
    public static LedgerResponseDTO from(Ledger ledger, String stockName) {
        // 거래 방향 계산: 매수는 음수 금액(debit), 매도는 양수 금액
        // 매수: 실제 지출 금액 (debit)
        // 매도: 실제 수취 금액 (credit)
        long netAmount = ledger.getDebit() != null ? ledger.getDebit() : ledger.getCredit();
        
        return LedgerResponseDTO.builder()
                .id(ledger.getId())
                .ticker(ledger.getTicker())
                .stockName(stockName)
                .transactionType(ledger.getTransactionType())
                .transactionTypeKorean(ledger.getTransactionType() != null 
                    ? ledger.getTransactionType().getKoreanName() : "")
                .quantity(ledger.getQtyChange())
                .price(ledger.getAmountChange())
                .totalAmount(ledger.getCredit() != null ? ledger.getCredit() : 0L)
                .commission(ledger.getCommission())
                .tax(ledger.getTax() != null ? ledger.getTax() : 0L)
                .netAmount(netAmount)
                .transactionDate(ledger.getCreatedAt())
                .build();
    }
}

