package com.beyond.MKX.common.auth.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.member.dto.ForgotPasswordReqDto;
import com.beyond.MKX.domain.member.dto.ResetPasswordReqDto;
import com.beyond.MKX.domain.member.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PASSWORD_RESET_CONTROLLER: 비밀번호 재설정 컨트롤러
 * - 비밀번호 재설정 요청 (이메일 발송)
 * - 비밀번호 재설정 (토큰 + 새 비밀번호)
 */
@Slf4j
@RestController
@RequestMapping("/auth/member")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * FORGOT_PASSWORD: 비밀번호 재설정 요청
     * POST /auth/member/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordReqDto request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ApiResponse.ok(null, "비밀번호 재설정 링크를 이메일로 보냈습니다.");
    }

    /**
     * RESET_PASSWORD: 비밀번호 재설정 처리
     * POST /auth/member/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordReqDto request) {
        passwordResetService.resetPassword(request.getToken(), request.getPassword());
        return ApiResponse.ok(null, "비밀번호가 성공적으로 변경되었습니다.");
    }
}

