package com.beyond.MKX.domain.admin.dto;

import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.corporation.entity.Status;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CorporationSignUpApprovalDetailDto {

    private UUID corporationId;
    private String nameKo;
    private String nameEng;
    private String ownerName;
    private String regNo;
    private Status status;
    private String rejectReason;
    private LocalDate estDate;
    private String roadAddress;
    private String detailAddress;
    private Long capital;
    private Long recentAnnualSales;
    private String businessRegistrationCert;
    private String corporateSealCert;
    private String logoUrl;
    private LocalDateTime createdAt;
    private AdminSummaryDto admin;

    public static CorporationSignUpApprovalDetailDto from(Corporation corporation, AdminSummaryDto admin) {
        return CorporationSignUpApprovalDetailDto.builder()
                .corporationId(corporation.getId())
                .nameKo(corporation.getNameKo())
                .nameEng(corporation.getNameEng())
                .ownerName(corporation.getOwnerName())
                .regNo(corporation.getRegNo())
                .status(corporation.getStatus())
                .rejectReason(corporation.getRejectReason())
                .estDate(corporation.getEstDate())
                .roadAddress(corporation.getRoadAddress())
                .detailAddress(corporation.getDetailAddress())
                .capital(corporation.getCapital())
                .recentAnnualSales(corporation.getRecentAnnualSales())
                .businessRegistrationCert(corporation.getBusinessRegistrationCert())
                .corporateSealCert(corporation.getCorporateSealCert())
                .logoUrl(corporation.getLogoUrl())
                .createdAt(corporation.getCreatedAt())
                .admin(admin)
                .build();
    }
}
