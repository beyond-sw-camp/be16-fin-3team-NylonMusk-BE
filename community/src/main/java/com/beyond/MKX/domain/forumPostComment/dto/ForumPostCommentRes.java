package com.beyond.MKX.domain.forumPostComment.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder(toBuilder = true)
public record ForumPostCommentRes(
        UUID id,
        UUID postId,
        UUID createdBy,
        String content,
        Integer likes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean likedByMe
) {}