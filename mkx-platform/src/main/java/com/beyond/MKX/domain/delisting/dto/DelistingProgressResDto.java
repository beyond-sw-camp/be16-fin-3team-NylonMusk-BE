package com.beyond.MKX.domain.delisting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record DelistingProgressResDto(
        UUID stockId,
        String ticker,
        String nameKo,
        String currentStatus,
        String progressStage,
        List<ViolationSummaryDto> unresolvedViolations,
        List<String> nextSteps,
        CompensationStatusDto compensationStatus,
        LocalDateTime estimatedDelistingDate,
        LocalDateTime lastAnalysisDate,
        Integer totalViolationCount,
        Integer criticalViolationCount,
        BigDecimal overallRiskScore,
        String riskLevel,
        List<String> recommendations
) {}
