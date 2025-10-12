package com.beyond.MKX.domain.member.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

/** ordering-service 내부 회원 계좌 조회 Feign */
@FeignClient(name = "ordering-service", contextId = "memberAccountInternalClient")
public interface MemberAccountInternalClient {

    @GetMapping("/api/internal/member-accounts/{memberId}")
    MemberAccountSummaryRes getByMember(@PathVariable("memberId") UUID memberId);

    record MemberAccountSummaryRes(String accountNumber, String status) {}

    @GetMapping("/api/internal/member-accounts/by-number/{accountNumber}")
    MemberAccountByNumberRes getByAccountNumber(@PathVariable("accountNumber") String accountNumber);

    record MemberAccountByNumberRes(
            UUID accountId,
            UUID memberId,
            UUID brokerageId,
            String accountNumber,
            String status
    ) {}
}
