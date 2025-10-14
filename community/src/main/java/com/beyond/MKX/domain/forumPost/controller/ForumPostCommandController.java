package com.beyond.MKX.domain.forumPost.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import com.beyond.MKX.domain.forumPost.dto.ForumPostStatusUpdateReq;
import com.beyond.MKX.domain.forumPost.dto.ForumPostUpdateReq;
import com.beyond.MKX.domain.forumPost.service.command.ForumPostCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/forum/posts")
@RequiredArgsConstructor
public class ForumPostCommandController {

    private final ForumPostCommandService service;

    /** 글 수정 (작성자만) */
    @PatchMapping("/{postId}")
    public ResponseEntity<?> update(@PathVariable UUID postId,
                                    @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
                                    @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                    @Valid @RequestBody ForumPostUpdateReq req) {
        if (!StringUtils.hasText(userIdHeader)) {
            return ApiResponse.ok(null, "인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }
        ForumPostResDto res = service.update(postId, UUID.fromString(userIdHeader), roleHeader, req);
        return ApiResponse.ok(res, "게시글 수정 성공");
    }

    /** 글 삭제 (작성자 또는 관리자) */
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> delete(@PathVariable UUID postId,
                                    @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
                                    @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        if (!StringUtils.hasText(userIdHeader)) {
            return ApiResponse.ok(null, "인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }
        service.delete(postId, UUID.fromString(userIdHeader), roleHeader);
        return ApiResponse.noContent(null, "게시글 삭제(소프트) 성공");
    }

    /** 글 상태 수정 (관리자만) */
    @PatchMapping("/{postId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID postId,
                                          @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                          @Valid @RequestBody ForumPostStatusUpdateReq req) {
        ForumPostResDto res = service.updateStatus(postId, roleHeader, req);
        return ApiResponse.ok(res, "게시글 상태 변경 성공");
    }
}
