package com.beyond.MKX.domain.account.accountlist.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * account_list 검색 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class AccountListSearchReqDto {
    private String type;         // EXCHANGE | BROKERAGE | CORPORATION | MEMBER (optional)
    private String status;       // PENDING | APPROVED | REJECTED | SUSPENDED (optional)
    private String accountNumber; // 부분 일치 검색어 (optional)
}

