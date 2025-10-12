package com.beyond.MKX.domain.member.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.BrokerageOnly;
import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.domain.member.dto.MemberAccountAdminSummaryDto;
import com.beyond.MKX.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 증권사 관리자용 고객 계좌 목록 조회
 * - 본인 소속 증권사의 회원 계좌 요약을 조회한다.
 * - 필터: status(계좌 상태), search(계좌번호 부분 일치)
 */
@RestController
@RequestMapping("/admin/member-accounts")
@RequiredArgsConstructor
public class BrokerageMemberAccountAdminController {

    private final MemberService memberService;

    @BrokerageOnly
    @GetMapping
    public ResponseEntity<?> list(
            @AuthenticationPrincipal CustomAdminPrincipal principal,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search
    ) {
        List<MemberAccountAdminSummaryDto> result = memberService.getMemberAccountsForBrokerage(principal.id(), status, search);
        return ApiResponse.ok(result, "증권사 고객 계좌 목록 조회");
    }
}

