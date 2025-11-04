package com.beyond.MKX.domain.account.member.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class AmountRequest {
    @NotNull
    @Positive
    private Long amount;
    
    private UUID counterpartyAccountId;  // 상대 계좌 UUID (선택적)
    
    private String description;           // 거래 설명/transactionType (선택적, 예: "IPO_PAID", "IPO_REFUND")
    
    private String ticker;                // 종목 코드 (선택적, IPO 관련 거래 시 사용)
    
    // @JsonCreator: Jackson이 BigInteger 또는 Long을 모두 받아서 Long으로 변환
    @JsonCreator
    public AmountRequest(
            @JsonProperty("amount") Object amount,  // BigInteger 또는 Long을 받을 수 있도록 Object로 받음
            @JsonProperty("counterpartyAccountId") UUID counterpartyAccountId,
            @JsonProperty("description") String description,
            @JsonProperty("ticker") String ticker) {
        // BigInteger 또는 Long을 Long으로 변환
        if (amount instanceof BigInteger) {
            this.amount = ((BigInteger) amount).longValue();
        } else if (amount instanceof Number) {
            this.amount = ((Number) amount).longValue();
        } else {
            this.amount = null;
        }
        this.counterpartyAccountId = counterpartyAccountId;
        this.description = description;
        this.ticker = ticker;
    }
    
    // 일반 생성자 (Long amount용)
    public AmountRequest(Long amount, UUID counterpartyAccountId, String description) {
        this.amount = amount;
        this.counterpartyAccountId = counterpartyAccountId;
        this.description = description;
        this.ticker = null;
    }
    
    // 일반 생성자 (Long amount, ticker 포함)
    public AmountRequest(Long amount, UUID counterpartyAccountId, String description, String ticker) {
        this.amount = amount;
        this.counterpartyAccountId = counterpartyAccountId;
        this.description = description;
        this.ticker = ticker;
    }
}

