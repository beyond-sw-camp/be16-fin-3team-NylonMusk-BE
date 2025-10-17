package com.beyond.MKX.domain.forumPostLike.dto;

import lombok.Builder;

/** 게시글 좋아요 토글 응답 DTO */
@Builder
public record PostLikeToggleRes(
        boolean liked,  // true=좋아요 설정됨, false=좋아요 해제됨
        int likeCount   // 현재 총 좋아요 수
) {}
