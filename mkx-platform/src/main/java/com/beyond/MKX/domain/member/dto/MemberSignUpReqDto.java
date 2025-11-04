package com.beyond.MKX.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class MemberSignUpReqDto {

    @NotNull(message = "증권사 식별자는 필수입니다.")
    private UUID brokerageId;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 10, message = "이름은 10자 이내여야 합니다.")
    private String name;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 50, message = "이메일은 50자 이내여야 합니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이내여야 합니다.")
    private String password;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Size(max = 15, message = "전화번호는 15자 이내여야 합니다.")
    private String phone;

    /** 생년월일 (신분증 OCR로 추출, 선택적) */
    private LocalDate birthDate;
}
