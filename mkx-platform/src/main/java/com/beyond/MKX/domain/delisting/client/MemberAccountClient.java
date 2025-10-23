package com.beyond.MKX.domain.delisting.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
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
@FeignClient(name = "ordering-service", path = "/api/internal/accounts")
public interface MemberAccountClient {

    /**
     * 회원 계좌 입금
     * 
     * @param request 입금 요청 정보
     * @return 입금 결과
     */
    @PostMapping("/deposit")
    DepositResult deposit(@RequestBody DepositRequest request);

    /**
     * 입금 요청 DTO
     */
    record DepositRequest(
            UUID memberAccountId,
            BigDecimal amount,
            String description
    ) {}

    /**
     * 입금 결과 DTO
     */
    record DepositResult(
            boolean success,
            String message,
            BigDecimal newBalance
    ) {}
}
