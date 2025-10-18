package com.beyond.MKX.domain.disclosure.dto;

import com.beyond.MKX.domain.disclosure.entity.DisclosureStatus;
import com.beyond.MKX.domain.disclosure.entity.DisclosureType;

import java.time.LocalDateTime;
import java.util.UUID;

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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

