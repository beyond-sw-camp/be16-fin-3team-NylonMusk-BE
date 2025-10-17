package com.beyond.MKX.domain.forumVote.dto;

import lombok.Builder;
import java.util.UUID;

@Builder
public record ForumVoteSelectionResDto(
        UUID id,
        String text,
        int sortOrder,
        int votesCount
) {}
