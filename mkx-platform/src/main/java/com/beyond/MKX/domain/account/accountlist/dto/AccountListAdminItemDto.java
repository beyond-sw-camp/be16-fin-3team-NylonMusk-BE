package com.beyond.MKX.domain.account.accountlist.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드 응답에서 제외
public class AccountListAdminItemDto {
    private String accountNumber;
    private String type;
    private String status;
    private LocalDateTime createdAt;

    // null일 수도 있음
    private String memberName;
    private String memberEmail;
    private String corporationName;
    private String brokerageName;
}
