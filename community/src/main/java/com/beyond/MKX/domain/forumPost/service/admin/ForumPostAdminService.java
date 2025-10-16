package com.beyond.MKX.domain.forumPost.service.admin;

import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ForumPostAdminService {
    Page<ForumPostResDto> listAllIncludingDeleted(Pageable pageable);
    Page<ForumPostResDto> listDeleted(Pageable pageable);
    ForumPostResDto getIncludingDeleted(UUID id);
    void restore(UUID id);
    void hardDelete(UUID id);
}
