package com.beyond.MKX.domain.forumVote.service.command;

import com.beyond.MKX.domain.forumVote.dto.*;

import java.util.List;
import java.util.UUID;

public interface ForumVoteCommandService {
    ForumVoteResDto create(ForumVoteCreateReqDto req);
    ForumVoteCastResDto cast(UUID voteId, UUID actorId, ForumVoteCastReqDto req); // 제출/변경
    ForumVoteCastResDto revoke(UUID voteId, UUID actorId); // 전체 철회
    void reorderSelections(UUID voteId, UUID actorId, String actorRole, List<UUID> orderedIds);
}
