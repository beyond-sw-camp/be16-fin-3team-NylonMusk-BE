package com.beyond.MKX.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RESEND_VERIFICATION_EMAIL_REQ_DTO: 인증 이메일 재발송 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ResendVerificationEmailReqDto {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
}

