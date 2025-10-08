package com.beyond.MKX.domain.member.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.common.auth.security.ExchangeOrBrokerage;
import com.beyond.MKX.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class MemberAdminController {

    private final MemberService memberService;

    @ExchangeOrBrokerage
    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal CustomAdminPrincipal principal) {
        if (principal == null) {
            throw new BadCredentialsException("로그인이 필요합니다.");
        }
        return ApiResponse.ok(
                memberService.getMembersForAdmin(principal.id(), principal.role()),
                "회원 목록 조회"
        );
    }

    @ExchangeOrBrokerage
    @GetMapping("/{memberId}")
    public ResponseEntity<?> detail(@AuthenticationPrincipal CustomAdminPrincipal principal,
                                    @PathVariable("memberId") UUID memberId) {
        if (principal == null) {
            throw new BadCredentialsException("로그인이 필요합니다.");
        }
        return ApiResponse.ok(
                memberService.getMemberDetailForAdmin(principal.id(), principal.role(), memberId),
                "회원 상세 조회"
        );
    }
}
