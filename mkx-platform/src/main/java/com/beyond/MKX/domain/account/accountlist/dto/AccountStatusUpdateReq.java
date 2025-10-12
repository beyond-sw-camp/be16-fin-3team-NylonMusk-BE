package com.beyond.MKX.domain.account.accountlist.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatusUpdateReq {
    private String status; // APPROVED, SUSPENDED, REJECTED
}