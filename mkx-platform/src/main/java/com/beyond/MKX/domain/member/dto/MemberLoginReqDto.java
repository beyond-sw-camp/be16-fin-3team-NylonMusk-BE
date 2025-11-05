package com.beyond.MKX.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MemberLoginReqDto {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    /** CAPTCHA: CAPTCHA 키 (5회 이상 실패 시 필수) */
    private String captchaKey;

    /** CAPTCHA: CAPTCHA 값 (5회 이상 실패 시 필수) */
    private String captchaValue;
    
    /** CAPTCHA: CAPTCHA 타입 (image 또는 audio, 5회 이상 실패 시 필수) */
    private String captchaType;
}
