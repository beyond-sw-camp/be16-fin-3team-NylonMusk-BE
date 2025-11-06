package com.beyond.MKX.common.auth.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.exception.DuplicateResourceException;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.corporation.dto.CorporationSignUpReqDto;
import com.beyond.MKX.domain.corporation.service.CorporationService;
import com.beyond.MKX.domain.member.dto.MemberResDto;
import com.beyond.MKX.domain.member.dto.MemberSignUpReqDto;
import com.beyond.MKX.domain.member.service.MemberService;
import com.beyond.MKX.domain.securities_firm.dto.SecuritiesFirmSignUpReqDto;
import com.beyond.MKX.domain.securities_firm.service.SecuritiesFirmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/signup")
@RequiredArgsConstructor
public class SignUpController {

    private final CorporationService corporationService;
    private final SecuritiesFirmService securitiesFirmService;
    private final MemberService memberService;

    // 기업 회원가입
    @PostMapping(value = "/corporation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> corporationSignUp(@Valid @ModelAttribute CorporationSignUpReqDto request) {
        Admin admin = corporationService.signUpAdmin(request);
        return ApiResponse.created(admin.getId(), "기업 관리자 가입 신청 완료");
    }

    // 증권사 회원가입
    @PostMapping(value = "/securities-firm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> securitiesFirmSignUp(@Valid @ModelAttribute SecuritiesFirmSignUpReqDto request) {
        Admin admin = securitiesFirmService.signUpAdmin(request);
        return ApiResponse.created(admin.getId(), "증권사 관리자 가입 신청 완료");
    }

    @PostMapping(value = "/member", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> memberSignUp(@Valid @RequestBody MemberSignUpReqDto request) {
        MemberResDto member = memberService.signUp(request);
        return ApiResponse.created(member, "회원 가입 완료");
    }

    /**
     * CHECK_EMAIL_DUPLICATE: 이메일 중복 여부 확인 (회원가입 전 실시간 체크)
     * GET /auth/signup/check-email?email=xxx
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmailDuplicate(@RequestParam String email) {
        boolean isDuplicate = memberService.checkEmailDuplicate(email);
        if (isDuplicate) {
            throw new DuplicateResourceException("이미 가입된 이메일입니다.");
        }
        return ApiResponse.ok(null, "사용 가능한 이메일입니다.");
    }
}
