package com.beyond.MKX.domain.admin.dto;

import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SecuritiesFirmSignUpApprovalDto {

    private UUID securitiesFirmId;
    private String nameKo;
    private String nameEng;
    private String ownerName;
    private String regNo;
    private SecuritiesFirm.Status status;
    private String rejectReason;
    private LocalDate establishedDate;
    private String roadAddress;
    private String detailAddress;
    private String financialInvestmentLicenseNo;
    private String financialInvestmentLicenseDoc;
    private String businessRegistrationCert;
    private String corporateSealCert;
    private Double exchangeFee;
    private LocalDateTime createdAt;
    private AdminSummaryDto admin;

    public static SecuritiesFirmSignUpApprovalDto from(SecuritiesFirm firm, AdminSummaryDto admin) {
        return SecuritiesFirmSignUpApprovalDto.builder()
                .securitiesFirmId(firm.getId())
                .nameKo(firm.getNameKo())
                .nameEng(firm.getNameEng())
                .ownerName(firm.getOwnerName())
                .regNo(firm.getRegNo())
                .status(firm.getStatus())
                .rejectReason(firm.getRejectReason())
                .establishedDate(firm.getEstablishedDate())
                .roadAddress(firm.getRoadAddress())
                .detailAddress(firm.getDetailAddress())
                .financialInvestmentLicenseNo(firm.getFinancialInvestmentLicenseNo())
                .financialInvestmentLicenseDoc(firm.getFinancialInvestmentLicenseDoc())
                .businessRegistrationCert(firm.getBusinessRegistrationCert())
                .corporateSealCert(firm.getCorporateSealCert())
                .exchangeFee(firm.getExchangeFee())
                .createdAt(firm.getCreatedAt())
                .admin(admin)
                .build();
    }
}
