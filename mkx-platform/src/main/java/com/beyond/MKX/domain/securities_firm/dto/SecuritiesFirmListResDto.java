package com.beyond.MKX.domain.securities_firm.dto;

import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record SecuritiesFirmListResDto(
        UUID id,
        String nameKo,
        String nameEng,
        String ownerName,
        String regNo,
        String status,
        String adminName,
        String adminEmail,
        String adminPhone,
        LocalDate establishedDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SecuritiesFirmListResDto from(SecuritiesFirm s) {
        return from(s, null, null, null);
    }

    public static SecuritiesFirmListResDto from(SecuritiesFirm s, String adminName, String adminEmail, String adminPhone) {
        return SecuritiesFirmListResDto.builder()
                .id(s.getId())
                .nameKo(s.getNameKo())
                .nameEng(s.getNameEng())
                .ownerName(s.getOwnerName())
                .regNo(s.getRegNo())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .adminName(adminName)
                .adminEmail(adminEmail)
                .adminPhone(adminPhone)
                .establishedDate(s.getEstablishedDate())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}

