package com.beyond.MKX.domain.forumPostComment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ForumPostCommentCreateReq(
        @NotNull UUID postId,
        @NotBlank String content
) {}