package com.beyond.MKX.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RESET_PASSWORD_REQ_DTO: 비밀번호 재설정 (토큰 + 새 비밀번호) DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ResetPasswordReqDto {

    @NotBlank(message = "토큰은 필수입니다.")
    private String token;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    @Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).*$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
    )
    private String password;
}

