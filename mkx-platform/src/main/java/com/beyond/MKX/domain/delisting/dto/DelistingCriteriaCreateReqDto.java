package com.beyond.MKX.domain.delisting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DelistingCriteriaCreateReqDto(
        @NotBlank @Size(max = 50) String criteriaCode,
        @NotBlank @Size(max = 100) String criteriaName,
        @NotNull com.beyond.MKX.domain.delisting.entity.CriteriaType criteriaType,
        BigDecimal thresholdValue,
        Integer thresholdPeriod,
        com.beyond.MKX.domain.delisting.entity.DelistingCriteria.ThresholdUnit thresholdUnit,
        @Size(max = 1000) String description,
        Boolean isActive,
        UUID createdBy,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo
) {}
