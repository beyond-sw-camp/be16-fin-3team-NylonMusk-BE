package com.beyond.MKX.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FORGOT_PASSWORD_REQ_DTO: 비밀번호 재설정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ForgotPasswordReqDto {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식을 입력해주세요.")
    private String email;
}

