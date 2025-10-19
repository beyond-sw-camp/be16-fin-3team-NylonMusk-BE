package com.beyond.MKX.domain.forumPost.dto;

import com.beyond.MKX.domain.common.entity.WriterRole;
import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryResDto;
import com.beyond.MKX.domain.forumPost.entity.PostStatus;
import com.beyond.MKX.domain.forumPostComment.dto.ForumPostCommentRes;
import com.beyond.MKX.domain.forumVote.dto.ForumVoteResDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
public record ForumPostResDto(
        UUID id,
        UUID stockId,
        UUID createdBy,
        String title,
        String contents,
        String imageUrl,
        PostStatus status,
        int likesCount,
        int commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        long version,
        List<ForumCategoryResDto> categories,
        WriterRole writerRole,
        boolean likedByMe,
        List<ForumPostCommentRes> comments,
        ForumVoteResDto vote
) {}
