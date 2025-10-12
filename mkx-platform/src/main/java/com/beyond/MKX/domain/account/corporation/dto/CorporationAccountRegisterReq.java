package com.beyond.MKX.domain.account.corporation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CorporationAccountRegisterReq {
    @NotBlank
    @Size(max = 20)
    private String accountNumber;
}

