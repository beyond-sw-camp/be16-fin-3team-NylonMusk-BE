package com.beyond.MKX.domain.delisting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DelistingViolationCreateReqDto(
        @NotNull UUID stockId,
        @NotNull UUID criteriaId,
        @Size(max = 50) String criteriaCode,
        @NotNull com.beyond.MKX.domain.delisting.entity.ViolationType violationType,
        BigDecimal currentValue,
        BigDecimal thresholdValue,
        Integer consecutivePeriods,
        @NotNull LocalDateTime violationDate,
        @Size(max = 1000) String description,
        Integer severityScore,
        com.beyond.MKX.domain.delisting.entity.DelistingViolation.DetectionMethod detectionMethod,
        Boolean requiresAction
) {}
