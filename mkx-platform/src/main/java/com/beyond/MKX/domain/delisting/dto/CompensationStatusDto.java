package com.beyond.MKX.domain.delisting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record CompensationStatusDto(
        Boolean isCompleted,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal unpaidAmount,
        String status,
        String failureReason,
        LocalDateTime lastPaymentDate,
        Integer retryCount
) {}
