package com.beyond.MKX.domain.account.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotBlank(message = "수취인 계좌번호는 필수입니다.")
    private String toAccountNumber;
    
    @NotNull(message = "이체 금액은 필수입니다.")
    @Positive(message = "이체 금액은 양수여야 합니다.")
    private Long amount;
}

