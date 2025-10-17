// com/beyond/MKX/domain/forumVote/service/query/ForumVoteQueryService.java
package com.beyond.MKX.domain.forumVote.service.query;

import com.beyond.MKX.domain.forumVote.dto.ForumVoteResDto;

import java.util.UUID;

public interface ForumVoteQueryService {
    ForumVoteResDto get(UUID voteId, UUID viewerId);
    ForumVoteResDto getByPost(UUID postId, UUID viewerId);
}
