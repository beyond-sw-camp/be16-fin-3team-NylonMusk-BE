package com.beyond.MKX.domain.forumVote.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/** 투표 제출/변경 요청 */
@Builder
public record ForumVoteCastReqDto(
        @NotEmpty List<UUID> selectionIds // 사용자가 선택한 선택지 ID들
) {}
