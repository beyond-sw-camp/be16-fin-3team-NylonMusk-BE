package com.beyond.MKX.domain.delisting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record GptAnalysisResultResDto(
        UUID id,
        UUID stockId,
        String criteriaCode,
        String analysisType,
        BigDecimal riskScore,
        String analysisDescription,
        String analysisReasoning,
        LocalDateTime analysisDate,
        String financialData,
        Boolean isSuccessful,
        String errorMessage,
        Long processingTimeMs,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version
) {}
