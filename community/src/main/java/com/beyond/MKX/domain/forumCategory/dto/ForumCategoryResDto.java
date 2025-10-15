package com.beyond.MKX.domain.forumCategory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ForumCategoryResDto(
        UUID id,
        String name,
        String description,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        long version
) {}
