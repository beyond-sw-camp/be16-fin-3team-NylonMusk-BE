package com.beyond.MKX.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AmountRequest {
    @NotNull
    @Positive
    private BigInteger amount;
    UUID counterpartyAccountId;
    String description;
    String ticker;  // 종목 코드 (선택적, IPO 관련 거래 시 사용)
}

