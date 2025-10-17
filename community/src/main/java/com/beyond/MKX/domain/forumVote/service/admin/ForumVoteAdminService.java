package com.beyond.MKX.domain.forumVote.service.admin;

import com.beyond.MKX.domain.forumVote.dto.ForumVoteResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ForumVoteAdminService {
    Page<ForumVoteResDto> listAll(Pageable pageable);
}
