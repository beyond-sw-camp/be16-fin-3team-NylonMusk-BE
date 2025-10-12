package com.beyond.MKX.domain.account.member.client;

import com.beyond.MKX.domain.account.member.dto.AccountListRegisterReq;
import com.beyond.MKX.domain.account.member.dto.AccountStatusUpdateReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 플랫폼(mkx-platform-service)의 account_list 등록 내부 API를 호출하는 Feign 클라이언트.
 * - 회원 계좌 생성 직후, 공통 계좌 메타(account_list)에 MEMBER 유형으로 등록하는 용도
 */
@FeignClient(name = "mkx-platform-service", contextId = "accountListClient")
public interface AccountListClient {

    /**
     * account_list 등록 요청(MEMBER 유형)
     *
     * @param request 계좌번호, 유형
     */
    @PostMapping("/api/internal/account-list/register")
    void registerAccount(@RequestBody AccountListRegisterReq request);

    // 상태 변경 추가
    @PostMapping("/api/internal/account-list/{accountNumber}/status")
    void updateAccountStatus(
            @PathVariable("accountNumber") String accountNumber,
            @RequestBody AccountStatusUpdateReq req
    );
}
