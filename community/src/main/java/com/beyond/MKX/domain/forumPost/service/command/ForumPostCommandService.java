package com.beyond.MKX.domain.forumPost.service.command;

import com.beyond.MKX.domain.forumPost.dto.ForumPostCreateReq;
import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import com.beyond.MKX.domain.forumPost.dto.ForumPostStatusUpdateReq;
import com.beyond.MKX.domain.forumPost.dto.ForumPostUpdateReq;

import java.util.UUID;

public interface ForumPostCommandService {
    ForumPostResDto create(UUID actorId, String actorRole, ForumPostCreateReq req);
    ForumPostResDto update(UUID postId, UUID actorId, String actorRole, ForumPostUpdateReq req);
    void delete(UUID postId, UUID actorId, String actorRole);
    ForumPostResDto updateStatus(UUID postId, String actorRole, ForumPostStatusUpdateReq req);
}
