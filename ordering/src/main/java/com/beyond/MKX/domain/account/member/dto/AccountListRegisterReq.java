package com.beyond.MKX.domain.account.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 거래소(mkx-platform)에 전송되는 account_list 등록 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountListRegisterReq {
    private String accountNumber; // 계좌번호
    private String accountType;
}