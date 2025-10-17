package com.beyond.MKX.domain.forumVote.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ForumVoteCreateReqDto(
        UUID forumPostId,
        @NotBlank String title,
        @Min(1) int allowMultipleCount,
        @NotEmpty List<String> selections // 선택지 텍스트 목록
) {}
