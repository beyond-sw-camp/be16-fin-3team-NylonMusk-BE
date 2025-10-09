package com.beyond.MKX.domain.account.member.dto;

import com.beyond.MKX.domain.account.member.entity.MemberAccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberAccountSummary {
    private String accountNumber;
    private MemberAccountStatus status;
}

