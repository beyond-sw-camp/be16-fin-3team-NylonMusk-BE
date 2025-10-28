package com.beyond.MKX.domain.delisting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ComprehensiveAnalysisResDto(
        UUID stockId,
        Integer totalAnalysisCount,
        List<String> analysisTypes,
        Map<String, GptAnalysisResultResDto> latestResultsByType,
        List<GptAnalysisResultResDto> allResults,
        BigDecimal overallRiskScore,
        String riskLevel,
        LocalDateTime lastAnalysisDate
) {}
