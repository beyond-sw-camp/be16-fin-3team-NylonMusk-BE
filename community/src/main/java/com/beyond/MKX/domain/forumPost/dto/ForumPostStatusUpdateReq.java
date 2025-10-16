package com.beyond.MKX.domain.forumPost.dto;

import com.beyond.MKX.domain.forumPost.entity.PostStatus;
import jakarta.validation.constraints.NotNull;

/** 상태 수정용 (관리자만 허용) */
public record ForumPostStatusUpdateReq(
        @NotNull PostStatus status
) {}
