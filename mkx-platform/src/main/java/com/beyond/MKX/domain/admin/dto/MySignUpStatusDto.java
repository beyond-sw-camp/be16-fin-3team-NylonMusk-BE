package com.beyond.MKX.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MySignUpStatusDto {
    private SignUpType type;
    private CorporationSignUpApprovalDetailDto corporation;
    private SecuritiesFirmSignUpApprovalDetailDto securitiesFirm;

    public static MySignUpStatusDto ofCorporation(CorporationSignUpApprovalDetailDto corporationDto) {
        return MySignUpStatusDto.builder()
                .type(SignUpType.CORPORATION)
                .corporation(corporationDto)
                .build();
    }

    public static MySignUpStatusDto ofSecuritiesFirm(SecuritiesFirmSignUpApprovalDetailDto securitiesFirmDto) {
        return MySignUpStatusDto.builder()
                .type(SignUpType.SECURITIES_FIRM)
                .securitiesFirm(securitiesFirmDto)
                .build();
    }
}
