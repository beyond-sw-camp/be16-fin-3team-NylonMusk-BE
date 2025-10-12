package com.beyond.MKX.domain.account.corporation.controller;

import com.beyond.MKX.common.auth.security.CorporationOnly;
import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.common.auth.security.ExchangeOnly;
import com.beyond.MKX.common.dto.AmountRequest;
import com.beyond.MKX.domain.account.corporation.dto.CorporationAccountRegisterReq;
import com.beyond.MKX.domain.account.corporation.entity.CorporationAccount;
import com.beyond.MKX.domain.account.corporation.service.CorporationAccountService;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.beyond.MKX.common.apiResponse.ApiResponse;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

/**
 * 기업 계좌 REST 컨트롤러
 *
 * 흐름
 * - 기업 관리자(@CorporationOnly)가 계좌번호를 제출해 등록(PENDING)한다.
 *   이때 FK 제약에 따라 account_list에도 동시에 등록된다(멱등 upsert).
 * - 거래소 관리자(@ExchangeOnly)가 승인/반려/정지를 처리한다.
 */
@RestController
@RequestMapping("/accounts/corporation")
@RequiredArgsConstructor
public class CorporationAccountController {

    private final CorporationAccountService service;
    private final AdminRepository adminRepository;

    /**
     * 기업 계좌 등록 요청 (JSON Body)
     * - 인증 주체의 소속 기업 ID를 사용하며, 파라미터로 corporationId를 받지 않는다(스푸핑 방지).
     */
    @PostMapping("/register")
    @CorporationOnly
    public ResponseEntity<CorporationAccount> register(
            @AuthenticationPrincipal CustomAdminPrincipal principal,
            @Valid @RequestBody CorporationAccountRegisterReq req
    ) {
        Admin admin = adminRepository.findById(principal.id())
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));
        if (admin.getCorporation() == null) {
            throw new IllegalArgumentException("기업 소속 관리자가 아닙니다.");
        }
        UUID corporationId = admin.getCorporation().getId();
        CorporationAccount acc = service.register(corporationId, req.getAccountNumber(), null);
        return ResponseEntity.ok(acc);
    }

    /** 거래소 승인 (계좌번호 기준) */
    @PostMapping("/{accountNumber}/approve")
    @ExchangeOnly
    public ResponseEntity<?> approve(@PathVariable String accountNumber) {
        service.approveByAccountNumber(accountNumber);
        return ApiResponse.ok(null, "기업 계좌 승인 완료");
    }

    /** 거래소 반려 (계좌번호 기준) */
    @PostMapping("/{accountNumber}/reject")
    @ExchangeOnly
    public ResponseEntity<?> reject(@PathVariable String accountNumber) {
        service.rejectByAccountNumber(accountNumber);
        return ApiResponse.ok(null, "기업 계좌 반려 완료");
    }

    /** 거래소 정지 (계좌번호 기준) */
    @PostMapping("/{accountNumber}/suspend")
    @ExchangeOnly
    public ResponseEntity<?> suspend(@PathVariable String accountNumber) {
        service.suspendByAccountNumber(accountNumber);
        return ApiResponse.ok(null, "기업 계좌 정지 완료");
    }

    /** 기업 계좌 단건 조회 (계좌번호) */
    @GetMapping("/by-number/{accountNumber}")
    @CorporationOnly
    public ResponseEntity<?> getByAccountNumber(@PathVariable String accountNumber) {
        CorporationAccount acc = service.getByAccountNumber(accountNumber);
        return ApiResponse.ok(acc, "기업 계좌 조회 성공");
    }

    /** 기업 계좌 입금  */
    @PostMapping("/{id}/deposit")
    @CorporationOnly
    public ResponseEntity<?> deposit(@PathVariable UUID id, @RequestBody AmountRequest req) {
        BigInteger balance = service.deposit(id, req.getAmount());
        return ApiResponse.ok(Map.of("balance", balance), "입금 완료");
    }

    /** 기업 계좌 출금  */
    @PostMapping("/{id}/withdraw")
    @CorporationOnly
    public ResponseEntity<?> withdraw(@PathVariable UUID id, @RequestBody AmountRequest req) {
        BigInteger balance = service.withdraw(id, req.getAmount());
        return ApiResponse.ok(Map.of("balance", balance), "출금 완료");
    }
}
