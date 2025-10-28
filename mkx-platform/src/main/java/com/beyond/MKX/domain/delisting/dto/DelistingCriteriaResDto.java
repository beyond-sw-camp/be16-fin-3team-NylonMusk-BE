package com.beyond.MKX.domain.delisting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record DelistingCriteriaResDto(
        UUID id,
        String criteriaCode,
        String criteriaName,
        com.beyond.MKX.domain.delisting.entity.CriteriaType criteriaType,
        BigDecimal thresholdValue,
        Integer thresholdPeriod,
        com.beyond.MKX.domain.delisting.entity.DelistingCriteria.ThresholdUnit thresholdUnit,
        String description,
        Boolean isActive,
        UUID createdBy,
        UUID updatedBy,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        long version
) {}
