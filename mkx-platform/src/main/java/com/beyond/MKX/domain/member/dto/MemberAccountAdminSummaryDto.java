package com.beyond.MKX.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberAccountAdminSummaryDto {
    private UUID memberId;
    private String name;
    private String email;
    private String accountNumber; // 회원 계좌번호
    private String accountStatus; // 회원 계좌 상태
}

