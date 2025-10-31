package com.beyond.MKX.domain.delisting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DelistingCriteriaUpdateReqDto(
        @Size(max = 100) String criteriaName,
        com.beyond.MKX.domain.delisting.entity.CriteriaType criteriaType,
        BigDecimal thresholdValue,
        com.beyond.MKX.domain.delisting.entity.DelistingCriteria.ComparisonOperator comparisonOperator,
        Integer thresholdPeriod,
        com.beyond.MKX.domain.delisting.entity.DelistingCriteria.ThresholdUnit thresholdUnit,
        @Size(max = 1000) String description,
        Boolean isActive,
        UUID updatedBy,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo
) {}
