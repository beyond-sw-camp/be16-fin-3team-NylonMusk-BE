package com.beyond.MKX.domain.corporation.dto;

import com.beyond.MKX.domain.corporation.entity.Corporation;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CorporationDetailResDto(
        UUID id,
        String nameKo,
        String nameEng,
        String ownerName,
        String regNo,
        String status,
        LocalDate estDate,
        String roadAddress,
        String detailAddress,
        Long capital,
        Long recentAnnualSales,
        String businessRegistrationCert,
        String corporateSealCert,
        String logoUrl,
        String rejectReason,
        String adminName,
        String adminEmail,
        String adminPhone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public static CorporationDetailResDto from(Corporation c) {
        return from(c, null, null, null);
    }

    public static CorporationDetailResDto from(Corporation c, String adminName, String adminEmail, String adminPhone) {
        return CorporationDetailResDto.builder()
            .id(c.getId())
            .nameKo(c.getNameKo())
            .nameEng(c.getNameEng())
            .ownerName(c.getOwnerName())
            .regNo(c.getRegNo())
            .status(c.getStatus() != null ? c.getStatus().name() : null)
            .estDate(c.getEstDate())
            .roadAddress(c.getRoadAddress())
            .detailAddress(c.getDetailAddress())
            .capital(c.getCapital())
            .recentAnnualSales(c.getRecentAnnualSales())
            .businessRegistrationCert(c.getBusinessRegistrationCert())
            .corporateSealCert(c.getCorporateSealCert())
            .logoUrl(c.getLogoUrl())
            .rejectReason(c.getRejectReason())
            .adminName(adminName)
            .adminEmail(adminEmail)
            .adminPhone(adminPhone)
            .createdAt(c.getCreatedAt())
            .updatedAt(c.getUpdatedAt())
            .deletedAt(c.getDeletedAt())
            .build();
    }
}
