package com.beyond.MKX.domain.forumPost.service.query;

import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import com.beyond.MKX.domain.forumPost.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ForumPostQueryService {
    Page<ForumPostResDto> list(PostStatus status, Pageable pageable);
    Page<ForumPostResDto> listMine(UUID me, PostStatus status, Pageable pageable);
    Page<ForumPostResDto> listByUser(UUID userId, PostStatus status, Pageable pageable);
    ForumPostResDto get(UUID postId);
}
