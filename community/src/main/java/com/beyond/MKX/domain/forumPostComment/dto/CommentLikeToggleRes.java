package com.beyond.MKX.domain.forumPostComment.dto;

import lombok.Builder;

@Builder
public record CommentLikeToggleRes(
        boolean liked,   // 토글 후 상태: true=좋아요, false=취소
        int likeCount    // 현재 총 좋아요 수
) {}
