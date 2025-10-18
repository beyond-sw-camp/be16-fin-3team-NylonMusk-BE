package com.beyond.MKX.domain.disclosure.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DuplicateDisclosureInfo(
        UUID id,
        String title,
        LocalDateTime createdAt
) {}

