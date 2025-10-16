// com/beyond/MKX/domain/forumPost/dto/ForumPostUpdateReq.java
package com.beyond.MKX.domain.forumPost.dto;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** 글 수정 요청 (작성자 전용). 제공된 필드만 변경한다. */
public record ForumPostUpdateReq(
        @Size(max = 255) String title,
        String contents,
        // 제공되면 카테고리를 "해당 목록으로 교체". null이면 카테고리 변경 없음.
        List<UUID> categoryIds,
        // 제공되면 이미지 교체(업로드). null/empty면 변경 없음.
        MultipartFile imageFile,
        // true면 기존 이미지 제거. imageFile과 동시 제공 시 imageFile이 우선.
        Boolean removeImage
) {}
