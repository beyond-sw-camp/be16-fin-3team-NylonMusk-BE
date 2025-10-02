package com.beyond.MKX.domain.securities_firm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class SecuritiesFirmSignUpReqDto {

    @NotBlank(message = "증권사 국문명은 필수입니다.")
    @Size(max = 50, message = "증권사 국문명은 50자 이하여야 합니다.")
    private String nameKo;

    @NotBlank(message = "증권사 영문명은 필수입니다.")
    @Size(max = 100, message = "증권사 영문명은 100자 이하여야 합니다.")
    private String nameEng;

    @NotBlank(message = "대표자명은 필수입니다.")
    @Size(max = 10, message = "대표자명은 10자 이하여야 합니다.")
    private String ownerName;

    @NotBlank(message = "사업자등록번호는 필수입니다.")
    @Size(max = 30, message = "사업자등록번호는 30자 이하여야 합니다.")
    private String regNo;

    @NotNull(message = "설립일은 필수입니다.")
    private LocalDate establishedDate;

    @NotBlank(message = "도로명주소는 필수입니다.")
    @Size(max = 50, message = "도로명주소는 50자 이하여야 합니다.")
    private String roadAddress;

    @NotBlank(message = "상세주소는 필수입니다.")
    @Size(max = 50, message = "상세주소는 50자 이하여야 합니다.")
    private String detailAddress;

    @NotBlank(message = "금융투자업 인가번호는 필수입니다.")
    @Size(max = 50, message = "금융투자업 인가번호는 50자 이하여야 합니다.")
    private String financialInvestmentLicenseNo;

    @NotBlank(message = "금융투자업 인가증 URL은 필수입니다.")
    @Size(max = 512, message = "금융투자업 인가증 URL은 512자 이하여야 합니다.")
    private String financialInvestmentLicenseDoc;

    @NotBlank(message = "사업자등록증 URL은 필수입니다.")
    @Size(max = 512, message = "사업자등록증 URL은 512자 이하여야 합니다.")
    private String businessRegistrationCert;

    @NotBlank(message = "법인인감증명서 URL은 필수입니다.")
    @Size(max = 512, message = "법인인감증명서 URL은 512자 이하여야 합니다.")
    private String corporateSealCert;

    @NotNull(message = "거래소 수수료율은 필수입니다.")
    private Double exchangeFee;

    @NotBlank(message = "관리자 이름은 필수입니다.")
    @Size(max = 10, message = "관리자 이름은 10자 이하여야 합니다.")
    private String adminName;

    @NotBlank(message = "관리자 이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 50, message = "관리자 이메일은 50자 이하여야 합니다.")
    private String adminEmail;

    @NotBlank(message = "관리자 전화번호는 필수입니다.")
    @Size(max = 20, message = "관리자 전화번호는 20자 이하여야 합니다.")
    private String adminPhone;

    @NotBlank(message = "관리자 비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "관리자 비밀번호는 8자 이상 20자 이하여야 합니다.")
    private String adminPassword;
}
