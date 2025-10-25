package com.beyond.MKX.domain.securities_firm.dto;

import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record SecuritiesFirmDetailResDto(
        UUID id,
        String nameKo,
        String nameEng,
        String ownerName,
        String regNo,
        String status,
        LocalDate establishedDate,
        String roadAddress,
        String detailAddress,
        String financialInvestmentLicenseNo,
        String financialInvestmentLicenseDoc,
        String businessRegistrationCert,
        String corporateSealCert,
        Double exchangeFee,
        String rejectReason,
        String adminName,
        String adminEmail,
        String adminPhone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SecuritiesFirmDetailResDto from(SecuritiesFirm s) {
        return from(s, null, null, null);
    }

    public static SecuritiesFirmDetailResDto from(SecuritiesFirm s, String adminName, String adminEmail, String adminPhone) {
        return SecuritiesFirmDetailResDto.builder()
                .id(s.getId())
                .nameKo(s.getNameKo())
                .nameEng(s.getNameEng())
                .ownerName(s.getOwnerName())
                .regNo(s.getRegNo())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .establishedDate(s.getEstablishedDate())
                .roadAddress(s.getRoadAddress())
                .detailAddress(s.getDetailAddress())
                .financialInvestmentLicenseNo(s.getFinancialInvestmentLicenseNo())
                .financialInvestmentLicenseDoc(s.getFinancialInvestmentLicenseDoc())
                .businessRegistrationCert(s.getBusinessRegistrationCert())
                .corporateSealCert(s.getCorporateSealCert())
                .exchangeFee(s.getExchangeFee())
                .rejectReason(s.getRejectReason())
                .adminName(adminName)
                .adminEmail(adminEmail)
                .adminPhone(adminPhone)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}

