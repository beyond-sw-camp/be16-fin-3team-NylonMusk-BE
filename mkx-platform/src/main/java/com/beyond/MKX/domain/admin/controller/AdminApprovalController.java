package com.beyond.MKX.domain.admin.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.CorporationOrBrokerage;
import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.common.auth.security.ExchangeOnly;
import com.beyond.MKX.domain.admin.dto.MySignUpStatusDto;
import com.beyond.MKX.domain.admin.dto.RejectRequestDto;
import com.beyond.MKX.domain.admin.service.AdminApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/approval-requests")
@RequiredArgsConstructor
public class AdminApprovalController {

    private final AdminApprovalService adminApprovalService;

    @ExchangeOnly
    @GetMapping("/corporations")
    public ResponseEntity<?> listPendingCorporations(@AuthenticationPrincipal CustomAdminPrincipal principal) {
        return ApiResponse.ok(
                adminApprovalService.getPendingCorporationSummaries(),
                "기업 가입 신청 목록 조회 성공"
        );
    }

    @ExchangeOnly
    @GetMapping("/corporations/{corporationId}")
    public ResponseEntity<?> getCorporationDetail(@AuthenticationPrincipal CustomAdminPrincipal principal,
                                                  @PathVariable UUID corporationId) {
        return ApiResponse.ok(
                adminApprovalService.getCorporationDetail(corporationId),
                "기업 가입 신청 상세 조회 성공"
        );
    }

    @ExchangeOnly
    @GetMapping("/securities-firms")
    public ResponseEntity<?> listPendingSecuritiesFirms(@AuthenticationPrincipal CustomAdminPrincipal principal) {
        return ApiResponse.ok(
                adminApprovalService.getPendingSecuritiesFirmSummaries(),
                "증권사 가입 신청 목록 조회 성공"
        );
    }

    @ExchangeOnly
    @GetMapping("/securities-firms/{securitiesFirmId}")
    public ResponseEntity<?> getSecuritiesFirmDetail(@AuthenticationPrincipal CustomAdminPrincipal principal,
                                                     @PathVariable UUID securitiesFirmId) {
        return ApiResponse.ok(
                adminApprovalService.getSecuritiesFirmDetail(securitiesFirmId),
                "증권사 가입 신청 상세 조회 성공"
        );
    }

    @ExchangeOnly
    @PostMapping("/corporations/{corporationId}/approve")
    public ResponseEntity<?> approveCorporation(@AuthenticationPrincipal CustomAdminPrincipal principal,
                                                @PathVariable UUID corporationId) {
        adminApprovalService.approveCorporation(corporationId);
        return ApiResponse.ok(null, "기업 가입 신청 승인 완료");
    }

    @ExchangeOnly
    @PostMapping("/corporations/{corporationId}/reject")
    public ResponseEntity<?> rejectCorporation(@AuthenticationPrincipal CustomAdminPrincipal principal,
                                               @PathVariable UUID corporationId,
                                               @Valid @RequestBody RejectRequestDto request) {
        adminApprovalService.rejectCorporation(corporationId, request.getReason());
        return ApiResponse.ok(null, "기업 가입 신청 거절 완료");
    }

    @ExchangeOnly
    @PostMapping("/securities-firms/{securitiesFirmId}/approve")
    public ResponseEntity<?> approveSecuritiesFirm(@AuthenticationPrincipal CustomAdminPrincipal principal,
                                                   @PathVariable UUID securitiesFirmId) {
        adminApprovalService.approveSecuritiesFirm(securitiesFirmId);
        return ApiResponse.ok(null, "증권사 가입 신청 승인 완료");
    }

    @ExchangeOnly
    @PostMapping("/securities-firms/{securitiesFirmId}/reject")
    public ResponseEntity<?> rejectSecuritiesFirm(@AuthenticationPrincipal CustomAdminPrincipal principal,
                                                  @PathVariable UUID securitiesFirmId,
                                                  @Valid @RequestBody RejectRequestDto request) {
        adminApprovalService.rejectSecuritiesFirm(securitiesFirmId, request.getReason());
        return ApiResponse.ok(null, "증권사 가입 신청 거절 완료");
    }

    @CorporationOrBrokerage
    @GetMapping("/me")
    public ResponseEntity<?> getMyApplication(@AuthenticationPrincipal CustomAdminPrincipal principal) {
        MySignUpStatusDto dto = adminApprovalService.getMySignUpStatus(principal.id());
        return ApiResponse.ok(dto, "내 가입 신청 상태 조회 성공");
    }
}
