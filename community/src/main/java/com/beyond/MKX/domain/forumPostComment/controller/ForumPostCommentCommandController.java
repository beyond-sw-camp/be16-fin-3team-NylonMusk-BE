package com.beyond.MKX.domain.forumPostComment.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.forumPostComment.dto.*;
import com.beyond.MKX.domain.forumPostComment.service.ForumPostCommentCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/forum/comments")
public class ForumPostCommentCommandController {

    private final ForumPostCommentCommandService commandService;

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

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ForumPostCommentCreateReq req
    ) {
        ForumPostCommentRes res = commandService.create(requireUser(userId), req);
        URI location = URI.create("/api/forum/comments/" + res.id());
        return ApiResponse.created(res, location, "댓글이 등록되었습니다.");
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<?> update(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID commentId,
            @Valid @RequestBody ForumPostCommentUpdateReq req
    ) {
        ForumPostCommentRes res = commandService.update(requireUser(userId), commentId, req);
        return ApiResponse.ok(res, "댓글이 수정되었습니다.");
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> delete(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID commentId
    ) {
        commandService.delete(requireUser(userId), commentId);
        return ApiResponse.ok(commentId, "댓글이 삭제되었습니다.");
    }

    /** 좋아요 토글: 이미 좋아요면 취소, 아니면 좋아요 */
    @PostMapping("/{commentId}/likes/toggle")
    public ResponseEntity<?> toggleLike(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID commentId
    ) {
        CommentLikeToggleRes res = commandService.toggleLike(requireUser(userId), commentId);
        String msg = res.liked() ? "좋아요가 설정되었습니다." : "좋아요가 해제되었습니다.";
        return ApiResponse.ok(res, "댓글 " + msg);
    }
}
