package com.beyond.MKX.domain.forumPost.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.UUID;
import com.beyond.MKX.domain.forumPost.entity.PostStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ForumPostResDto(
        UUID id,
        UUID stockId,
        UUID createdBy,
        String title,
        String contents,
        String imageUrl,
        PostStatus status,
        int likesCount,
        int commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        long version
) {}
