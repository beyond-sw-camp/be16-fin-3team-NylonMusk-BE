package com.beyond.MKX.domain.corporation.dto;

import com.beyond.MKX.domain.corporation.entity.Corporation;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CorporationListResDto(
        UUID id,
        String nameKo,
        String nameEng,
        String ownerName,
        String regNo,
        String status,
        String adminName,
        String adminEmail,
        String adminPhone,
        LocalDate estDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CorporationListResDto from(Corporation c) {
        return from(c, null, null, null);
    }

    public static CorporationListResDto from(Corporation c, String adminName, String adminEmail, String adminPhone) {
        return CorporationListResDto.builder()
            .id(c.getId())
            .nameKo(c.getNameKo())
            .nameEng(c.getNameEng())
            .ownerName(c.getOwnerName())
            .regNo(c.getRegNo())
            .status(c.getStatus() != null ? c.getStatus().name() : null)
            .adminName(adminName)
            .adminEmail(adminEmail)
            .adminPhone(adminPhone)
            .estDate(c.getEstDate())
            .createdAt(c.getCreatedAt())
            .updatedAt(c.getUpdatedAt())
            .build();
    }
}
