package com.beyond.MKX.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CorporationSignUpSummaryDto {
    private UUID corporationId;
    private String nameKo;
    private String ownerName;
    private String regNo;
    private LocalDateTime createdAt;
    private String adminName;
    private String adminEmail;
}
