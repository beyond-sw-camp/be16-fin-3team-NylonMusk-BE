package com.beyond.MKX.domain.account.accountlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * account_list 내부 등록용 최소 DTO
 * - 회원 계좌 생성 후 메타 등록에는 accountNumber만 필요.
 */
@Getter
@NoArgsConstructor
public class AccountListRegisterReqDto {

    @NotBlank(message = "accountNumber는 필수입니다.")
    @Size(max = 20, message = "accountNumber는 최대 20자입니다.")
    private String accountNumber;
}

