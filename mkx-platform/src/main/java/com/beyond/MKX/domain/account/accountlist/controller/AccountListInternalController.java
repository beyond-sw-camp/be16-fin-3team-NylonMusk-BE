package com.beyond.MKX.domain.account.accountlist.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.account.accountlist.dto.AccountListRegisterReqDto;
import com.beyond.MKX.domain.account.accountlist.dto.AccountStatusUpdateReq;
import com.beyond.MKX.domain.account.accountlist.entity.AccountType;
import com.beyond.MKX.domain.account.accountlist.service.AccountListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 내부용 account_list 등록 컨트롤러
 *
 * 용도
 * - 주문서비스 등 내부 시스템이 회원 계좌 생성 후 메타를 등록할 때 사용.
 * - 운영 환경에서는 게이트웨이 레벨에서 외부로부터 차단되어야 함.
 * - 본 컨트롤러는 최소 파라미터(accountNumber)만 검증하고, 유형은 MEMBER로 고정 등록한다.
 */
@RestController
@RequestMapping("/api/internal/account-list")
@RequiredArgsConstructor
public class AccountListInternalController {

    private final AccountListService service;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid AccountListRegisterReqDto request) {
        service.registerIfAbsent(request.getAccountNumber(), AccountType.MEMBER);
        return com.beyond.MKX.common.apiResponse.ApiResponse.ok(null, "account_list 등록 완료");
    }

    @PostMapping("/{accountNumber}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String accountNumber,
            @RequestBody AccountStatusUpdateReq req
    ) {
        service.updateStatusByAccountNumber(accountNumber, req.getStatus());
        return ApiResponse.ok(null, "account_list 상태 변경 완료");
    }
}
