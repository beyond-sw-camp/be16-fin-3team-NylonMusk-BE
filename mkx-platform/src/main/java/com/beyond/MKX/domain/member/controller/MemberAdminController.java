package com.beyond.MKX.domain.member.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.common.auth.security.ExchangeOrBrokerage;
import com.beyond.MKX.domain.member.service.MemberAdminQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class MemberAdminController {

    private final MemberAdminQueryService queryService;

    @ExchangeOrBrokerage
    @GetMapping
    public ResponseEntity<?> list(
            @AuthenticationPrincipal CustomAdminPrincipal principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (principal == null) {
            throw new BadCredentialsException("로그인이 필요합니다.");
        }
        Page<?> page = queryService.list(principal.id(), principal.role(), q, status, pageable);
        return ApiResponse.ok(page, "회원 목록 조회");
    }

    @ExchangeOrBrokerage
    @GetMapping("/{memberId}")
    public ResponseEntity<?> detail(@AuthenticationPrincipal CustomAdminPrincipal principal,
                                    @PathVariable("memberId") UUID memberId) {
        if (principal == null) {
            throw new BadCredentialsException("로그인이 필요합니다.");
        }
        return ApiResponse.ok(
                queryService.detail(principal.id(), principal.role(), memberId),
                "회원 상세 조회"
        );
    }
}
