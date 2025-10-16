package com.beyond.MKX.domain.forumCategory.service.query;

import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ForumCategoryQueryService {

    /** 단건(삭제 포함) */
    ForumCategoryResDto get(UUID id);

    /** 목록 (?deleted=all|only|exclude) */
    Page<ForumCategoryResDto> list(String deleted, Pageable pageable);

    /** 특정 사용자 기준 (uuid 또는 userId) */
    Page<ForumCategoryResDto> listByUser(UUID userUuid, String userId, String deleted, Pageable pageable);
}
