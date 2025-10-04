package com.beyond.MKX.domain.admin.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.admin.dto.MySignUpStatusDto;
import com.beyond.MKX.domain.admin.dto.RejectRequestDto;
import com.beyond.MKX.domain.admin.entity.Role;
import com.beyond.MKX.domain.admin.service.AdminApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/approval-requests")
@RequiredArgsConstructor
public class AdminApprovalController {

    private final AdminApprovalService adminApprovalService;

    @GetMapping("/corporations")
    public ResponseEntity<?> listPendingCorporations(@RequestHeader("X-User-Role") String roleHeader) {
        requireRole(roleHeader, Role.EXCHANGE);
        return ApiResponse.ok(
                adminApprovalService.getPendingCorporationSummaries(),
                "기업 가입 신청 목록 조회 성공"
        );
    }

    @GetMapping("/corporations/{corporationId}")
    public ResponseEntity<?> getCorporationDetail(@RequestHeader("X-User-Role") String roleHeader,
                                                  @PathVariable UUID corporationId) {
        requireRole(roleHeader, Role.EXCHANGE);
        return ApiResponse.ok(
                adminApprovalService.getCorporationDetail(corporationId),
                "기업 가입 신청 상세 조회 성공"
        );
    }

    @GetMapping("/securities-firms")
    public ResponseEntity<?> listPendingSecuritiesFirms(@RequestHeader("X-User-Role") String roleHeader) {
        requireRole(roleHeader, Role.EXCHANGE);
        return ApiResponse.ok(
                adminApprovalService.getPendingSecuritiesFirmSummaries(),
                "증권사 가입 신청 목록 조회 성공"
        );
    }

    @GetMapping("/securities-firms/{securitiesFirmId}")
    public ResponseEntity<?> getSecuritiesFirmDetail(@RequestHeader("X-User-Role") String roleHeader,
                                                     @PathVariable UUID securitiesFirmId) {
        requireRole(roleHeader, Role.EXCHANGE);
        return ApiResponse.ok(
                adminApprovalService.getSecuritiesFirmDetail(securitiesFirmId),
                "증권사 가입 신청 상세 조회 성공"
        );
    }

    @PostMapping("/corporations/{corporationId}/approve")
    public ResponseEntity<?> approveCorporation(@RequestHeader("X-User-Role") String roleHeader,
                                                @PathVariable UUID corporationId) {
        requireRole(roleHeader, Role.EXCHANGE);
        adminApprovalService.approveCorporation(corporationId);
        return ApiResponse.ok(null, "기업 가입 신청 승인 완료");
    }

    @PostMapping("/corporations/{corporationId}/reject")
    public ResponseEntity<?> rejectCorporation(@RequestHeader("X-User-Role") String roleHeader,
                                               @PathVariable UUID corporationId,
                                               @Valid @RequestBody RejectRequestDto request) {
        requireRole(roleHeader, Role.EXCHANGE);
        adminApprovalService.rejectCorporation(corporationId, request.getReason());
        return ApiResponse.ok(null, "기업 가입 신청 거절 완료");
    }

    @PostMapping("/securities-firms/{securitiesFirmId}/approve")
    public ResponseEntity<?> approveSecuritiesFirm(@RequestHeader("X-User-Role") String roleHeader,
                                                   @PathVariable UUID securitiesFirmId) {
        requireRole(roleHeader, Role.EXCHANGE);
        adminApprovalService.approveSecuritiesFirm(securitiesFirmId);
        return ApiResponse.ok(null, "증권사 가입 신청 승인 완료");
    }

    @PostMapping("/securities-firms/{securitiesFirmId}/reject")
    public ResponseEntity<?> rejectSecuritiesFirm(@RequestHeader("X-User-Role") String roleHeader,
                                                  @PathVariable UUID securitiesFirmId,
                                                  @Valid @RequestBody RejectRequestDto request) {
        requireRole(roleHeader, Role.EXCHANGE);
        adminApprovalService.rejectSecuritiesFirm(securitiesFirmId, request.getReason());
        return ApiResponse.ok(null, "증권사 가입 신청 거절 완료");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyApplication(@RequestHeader("X-User-Id") String userId,
                                              @RequestHeader("X-User-Role") String roleHeader) {
        Role role = parseRole(roleHeader);
        if (role != Role.CORPORATION && role != Role.BROKERAGE) {
            throw new IllegalArgumentException("가입 신청 상태 조회 권한이 없습니다.");
        }

        UUID adminId = parseUserId(userId);
        MySignUpStatusDto dto = adminApprovalService.getMySignUpStatus(adminId);
        return ApiResponse.ok(dto, "내 가입 신청 상태 조회 성공");
    }

    public UUID parseUserId(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식");
        }
    }

    public void requireRole(String roleHeader, Role expected) {
        Role role = parseRole(roleHeader);
        if (role != expected) {
            throw new IllegalArgumentException("요청 권한이 없습니다.");
        }
    }

    public Role parseRole(String roleHeader) {
        try {
            return Role.valueOf(roleHeader.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 사용자 역할 형식");
        }
    }
}
