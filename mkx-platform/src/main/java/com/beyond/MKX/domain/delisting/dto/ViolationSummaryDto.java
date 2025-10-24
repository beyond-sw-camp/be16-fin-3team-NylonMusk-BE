package com.beyond.MKX.domain.delisting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ViolationSummaryDto(
        String criteriaCode,
        String criteriaName,
        LocalDateTime violationDate,
        Integer consecutivePeriods,
        Integer severityScore,
        String description,
        Boolean gptAnalysisUsed,
        BigDecimal gptRiskScore
) {}
