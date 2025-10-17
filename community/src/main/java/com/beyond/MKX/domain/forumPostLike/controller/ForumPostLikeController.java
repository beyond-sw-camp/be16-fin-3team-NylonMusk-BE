package com.beyond.MKX.domain.forumPostLike.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.forumPostLike.dto.PostLikeToggleRes;
import com.beyond.MKX.domain.forumPostLike.service.ForumPostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 게시글 좋아요 토글 API
 * 경로 구조: /api/forum/posts/{postId}/likes/toggle
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/forum/posts")
public class ForumPostLikeController {

    private final ForumPostLikeService likeService;

    private UUID requireUser(String userHeader) {
        if (userHeader == null || userHeader.isBlank()) {
            throw new SecurityException("X-User-Id 헤더가 없습니다.");
        }
        try {
            return UUID.fromString(userHeader);
        } catch (IllegalArgumentException e) {
            throw new SecurityException("X-User-Id 형식이 올바르지 않습니다.");
        }
    }

    /** 좋아요 토글: 이미 좋아요면 취소, 아니면 좋아요 */
    @PostMapping("/{postId}/likes/toggle")
    public ResponseEntity<?> toggleLike(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID postId
    ) {
        PostLikeToggleRes res = likeService.toggleLike(requireUser(userId), postId);
        String msg = res.liked() ? "좋아요가 설정되었습니다." : "좋아요가 해제되었습니다.";
        return ApiResponse.ok(res, "게시글 " + msg);
    }
}
