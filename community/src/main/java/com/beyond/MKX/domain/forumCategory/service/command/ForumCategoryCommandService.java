package com.beyond.MKX.domain.forumCategory.service.command;

import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryCreateReqDto;
import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryResDto;
import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryUpdateReqDto;

import java.util.UUID;

public interface ForumCategoryCommandService {
    ForumCategoryResDto create(ForumCategoryCreateReqDto req);
    ForumCategoryResDto update(UUID id, ForumCategoryUpdateReqDto req);
    void softDelete(UUID id);
    ForumCategoryResDto restore(UUID id);
}

