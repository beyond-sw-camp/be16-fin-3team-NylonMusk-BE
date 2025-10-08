package com.beyond.MKX.domain.member.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.CustomMemberPrincipal;
import com.beyond.MKX.common.auth.security.MemberOnly;
import com.beyond.MKX.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @MemberOnly
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal CustomMemberPrincipal principal) {
        if (principal == null) {
            throw new BadCredentialsException("로그인이 필요합니다.");
        }
        return ApiResponse.ok(memberService.getProfile(principal.id()), "회원 정보 조회");
    }
}
