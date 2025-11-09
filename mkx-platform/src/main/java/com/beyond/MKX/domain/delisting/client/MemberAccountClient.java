package com.beyond.MKX.domain.delisting.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * 회원 계좌 서비스 FeignClient
 * 
 * 상장폐지 보상금 지급을 위해 회원 계좌에 입금하는 기능을 제공합니다.
 * ordering 서비스의 회원 계좌 관리 기능과 연동됩니다.
 * 
 * @author MKX Platform Team
 * @since 2025-01-15
 */
@FeignClient(name = "ordering-service", path = "/api/internal/member-accounts", contextId = "delistingMemberAccountClient", url = "${feign.client.url.ordering-service}")
public interface MemberAccountClient {

    /**
     * 계좌번호 조회 (내부용)
     * 
     * @param memberAccountId 회원 계좌 ID
     * @return 계좌번호
     */
    @GetMapping("/{memberAccountId}/account-number")
    Map<String, Object> getAccountNumber(@PathVariable UUID memberAccountId);

    /**
     * 회원 계좌 입금 (내부용) - 계좌번호 기준
     * 
     * @param accountNumber 계좌번호
     * @param request 입금 요청 정보
     * @return 입금 결과 (CommonDTO 형식: { result: { success, message, newBalance } })
     */
    @PostMapping("/by-number/{accountNumber}/deposit")
    Map<String, Object> depositByAccountNumber(@PathVariable String accountNumber, @RequestBody DepositRequest request);

    /**
     * 입금 요청 DTO
     */
    record DepositRequest(
            Long amount
    ) {}

    /**
     * 입금 결과 DTO
     */
    record DepositResult(
            boolean success,
            String message,
            BigDecimal newBalance
    ) {}
    
    /**
     * 계좌번호 조회 결과 DTO
     */
    record AccountNumberResult(
            boolean success,
            String message,
            String accountNumber
    ) {}
}
