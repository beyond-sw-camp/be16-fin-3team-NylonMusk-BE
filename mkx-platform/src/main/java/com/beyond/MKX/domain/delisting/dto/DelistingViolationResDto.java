package com.beyond.MKX.domain.delisting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record DelistingViolationResDto(
        UUID id,
        UUID stockId,
        UUID criteriaId,
        String criteriaCode,
        com.beyond.MKX.domain.delisting.entity.ViolationType violationType,
        BigDecimal currentValue,
        BigDecimal thresholdValue,
        Integer consecutivePeriods,
        LocalDateTime violationDate,
        Boolean isResolved,
        LocalDateTime resolvedDate,
        UUID resolvedBy,
        String description,
        Integer severityScore,
        com.beyond.MKX.domain.delisting.entity.DelistingViolation.DetectionMethod detectionMethod,
        Boolean requiresAction,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        long version
) {}
