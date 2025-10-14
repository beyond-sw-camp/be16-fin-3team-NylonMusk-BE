package com.beyond.MKX.domain.forumPost.dto;

import jakarta.validation.constraints.Size;

/** 글 내용 수정용 (작성자만 허용, status는 여기서 못 바꿈) */
public record ForumPostUpdateReq(
        @Size(max = 255) String title,
        String contents,
        @Size(max = 512) String imageUrl
) {}
