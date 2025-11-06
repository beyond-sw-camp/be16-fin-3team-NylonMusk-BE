package com.beyond.MKX.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * EMAIL_VERIFICATION_REQ_DTO: 이메일 인증 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class EmailVerificationReqDto {

    @NotBlank(message = "인증 토큰은 필수입니다.")
    private String token;
}

