package com.beyond.MKX.domain.account.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountInfoResponse {
    private String accountNumber;
    private String accountHolderName;  // 계좌 소유자 이름
    private String accountType;        // MEMBER, CORPORATION, BROKERAGE, EXCHANGE
}

