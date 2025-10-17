package com.beyond.MKX.domain.forumVote.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/** 투표 제출 결과 응답 */
@Builder
public record ForumVoteCastResDto(
        UUID voteId,
        int totalVoters,
        List<UUID> mySelections // 사용자가 최종적으로 선택한 선택지 ID 목록
) {}
