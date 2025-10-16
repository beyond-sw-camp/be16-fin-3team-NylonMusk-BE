package com.beyond.MKX.domain.forumPostComment.service;

import com.beyond.MKX.domain.forumPostComment.dto.ForumPostCommentRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ForumPostCommentQueryService {
    Page<ForumPostCommentRes> listByPost(UUID postId, Pageable pageable, UUID viewerId);
    Page<ForumPostCommentRes> listByUser(UUID userId, Pageable pageable, UUID viewerId);
    ForumPostCommentRes get(UUID commentId, UUID viewerId);
}
