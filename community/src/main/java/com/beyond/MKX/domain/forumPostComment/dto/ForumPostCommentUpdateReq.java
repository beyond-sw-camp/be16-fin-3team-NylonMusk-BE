package com.beyond.MKX.domain.forumPostComment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder(toBuilder = true)
public record ForumPostCommentUpdateReq(
        @NotBlank String content
) {}