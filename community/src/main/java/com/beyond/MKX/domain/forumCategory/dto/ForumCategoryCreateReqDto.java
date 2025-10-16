package com.beyond.MKX.domain.forumCategory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ForumCategoryCreateReqDto(
        @NotBlank @Size(max = 25) String name,
        @NotBlank @Size(max = 255) String description,
        UUID createdBy,          // 선택: 관리자/작성자 UUID
        String createdByUserId   // 선택: 관리자 계정 ID 등
) {}
