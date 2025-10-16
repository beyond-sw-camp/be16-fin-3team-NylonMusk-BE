package com.beyond.MKX.domain.forumPostComment.service;

import com.beyond.MKX.domain.forumPostComment.dto.*;

import java.util.UUID;

public interface ForumPostCommentCommandService {
    ForumPostCommentRes create(UUID requesterId, ForumPostCommentCreateReq req);
    ForumPostCommentRes update(UUID requesterId, UUID commentId, ForumPostCommentUpdateReq req);
    void delete(UUID requesterId, UUID commentId);
    CommentLikeToggleRes toggleLike(UUID requesterId, UUID commentId);
}
