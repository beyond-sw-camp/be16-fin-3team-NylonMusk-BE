package com.beyond.MKX.domain.disclosure.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.beyond.MKX.domain.disclosure.entity.DisclosureStatus;
import com.beyond.MKX.domain.disclosure.entity.DisclosureRejectCode;
import com.beyond.MKX.domain.disclosure.entity.DisclosureType;
import com.beyond.MKX.domain.disclosure.entity.DisclosureRelationType;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DisclosureResDto(
        UUID id,
        UUID stockId,
        DisclosureType disclosureType,
        String title,
        String summary,
        String fileUrl,
        DisclosureStatus status,
        LocalDateTime publishedAt,
        String stockNameSnapshot,
        String tickerSnapshot,
        DisclosureRejectCode rejectCode,
        String rejectReason,
        DisclosureRelationType relationType,
        String displayNo,
        Integer revisionNo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
