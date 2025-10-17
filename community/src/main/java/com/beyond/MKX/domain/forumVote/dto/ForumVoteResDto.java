package com.beyond.MKX.domain.forumVote.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record ForumVoteResDto(
        UUID id,
        UUID forumPostId,
        String title,
        int allowMultipleCount,
        int totalVoters,
        boolean votedByMe,
        List<ForumVoteSelectionResDto> selections,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version
) {}
