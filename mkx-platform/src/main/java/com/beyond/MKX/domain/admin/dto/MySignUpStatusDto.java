package com.beyond.MKX.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MySignUpStatusDto {
    private String type; // CORPORATION or SECURITIES_FIRM
    private CorporationSignUpApprovalDto corporation;
    private SecuritiesFirmSignUpApprovalDto securitiesFirm;

    public static MySignUpStatusDto ofCorporation(CorporationSignUpApprovalDto corporationDto) {
        return MySignUpStatusDto.builder()
                .type("CORPORATION")
                .corporation(corporationDto)
                .build();
    }

    public static MySignUpStatusDto ofSecuritiesFirm(SecuritiesFirmSignUpApprovalDto securitiesFirmDto) {
        return MySignUpStatusDto.builder()
                .type("SECURITIES_FIRM")
                .securitiesFirm(securitiesFirmDto)
                .build();
    }
}
