package com.beyond.MKX.domain.forumCategory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ForumCategoryUpdateReqDto(
        @Size(max = 25) String name,
        @Size(max = 255) String description
) {}
