package com.beyond.MKX.common.auth.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.member.dto.EmailVerificationReqDto;
import com.beyond.MKX.domain.member.dto.ResendVerificationEmailReqDto;
import com.beyond.MKX.domain.member.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EMAIL_VERIFICATION_CONTROLLER: 이메일 인증 컨트롤러
 * - 이메일 인증 토큰 검증
 * - 인증 이메일 재발송
 */
@Slf4j
@RestController
@RequestMapping("/auth/member")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    /**
     * VERIFY_EMAIL: 이메일 인증 처리
     * POST /auth/member/verify-email
     */
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody EmailVerificationReqDto request) {
        var member = emailVerificationService.verifyEmail(request.getToken());
        return ApiResponse.ok(
            new EmailVerificationResponse(member.getEmail()),
            "이메일 인증이 완료되었습니다."
        );
    }

    /**
     * RESEND_VERIFICATION_EMAIL: 인증 이메일 재발송
     * POST /auth/member/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(@Valid @RequestBody ResendVerificationEmailReqDto request) {
        emailVerificationService.resendVerificationEmail(request.getEmail());
        return ApiResponse.ok(null, "인증 이메일을 재발송했습니다.");
    }

    /**
     * EMAIL_VERIFICATION_RESPONSE: 이메일 인증 응답 DTO
     */
    public record EmailVerificationResponse(String email) {}
}

