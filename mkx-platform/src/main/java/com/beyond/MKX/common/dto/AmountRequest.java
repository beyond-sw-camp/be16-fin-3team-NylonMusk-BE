package com.beyond.MKX.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Getter
@NoArgsConstructor
public class AmountRequest {
    @NotNull
    @Positive
    private BigInteger amount;
}

