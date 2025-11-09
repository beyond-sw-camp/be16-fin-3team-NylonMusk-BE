package com.beyond.MKX.domain.member.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.time.LocalDateTime;
import java.util.UUID;

/** ordering-service 내부 회원 계좌 조회 Feign */
@FeignClient(name = "ordering-service", contextId = "memberAccountInternalClient", url = "${feign.client.url.ordering-service}")
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

    @GetMapping("/api/internal/member-accounts/{memberId}/detail")
    MemberAccountDetailRes getDetailByMember(@PathVariable("memberId") UUID memberId);

    record MemberAccountDetailRes(
            UUID accountId,
            UUID memberId,
            UUID brokerageId,
            String accountNumber,
            String status,
            Long balance,
            Long availableBalance,
            LocalDateTime createdAt
    ) {}
}
