package com.beyond.MKX.domain.forumPostComment.dto;

import com.beyond.MKX.domain.forumPostComment.entity.ForumPostComment;

public final class ForumPostCommentMapper {
    private ForumPostCommentMapper() {}

    public static ForumPostCommentRes toRes(ForumPostComment c) {
        return toRes(c, false); // 기본 false
    }

    public static ForumPostCommentRes toRes(ForumPostComment c, boolean likedByMe) {
        return ForumPostCommentRes.builder()
                .id(c.getId())
                .postId(c.getPost() != null ? c.getPost().getId() : null)
                .createdBy(c.getCreatedBy())
                .content(c.getContent())
                .likes(c.getLikes())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .likedByMe(likedByMe)
                .build();
    }
}
