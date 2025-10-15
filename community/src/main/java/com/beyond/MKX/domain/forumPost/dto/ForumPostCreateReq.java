package com.beyond.MKX.domain.forumPost.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public record ForumPostCreateReq(
        @NotNull UUID stockId,
        @NotNull @Size(min = 1, max = 120) String title,
        @NotNull @Size(min = 1) String contents,
        @NotEmpty(message = "카테고리를 최소 1개 이상 선택하세요.")
        List<UUID> categoryIds,
        MultipartFile imageFile
) {}
