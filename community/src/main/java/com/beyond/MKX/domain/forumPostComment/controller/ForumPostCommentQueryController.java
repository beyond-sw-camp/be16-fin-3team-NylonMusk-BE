package com.beyond.MKX.domain.forumPostComment.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.forumPostComment.dto.ForumPostCommentRes;
import com.beyond.MKX.domain.forumPostComment.service.ForumPostCommentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/forum/comments")
public class ForumPostCommentQueryController {

    private final ForumPostCommentQueryService queryService;

    private UUID parseViewer(String userHeader) {
        if (userHeader == null || userHeader.isBlank()) return null;
        try {
            return UUID.fromString(userHeader);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 게시글별 댓글 목록 조회 */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<?> listByPost(
            @PathVariable UUID postId,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            Pageable pageable
    ) {
        UUID viewerId = parseViewer(userHeader);
        Page<ForumPostCommentRes> page = queryService.listByPost(postId, pageable, viewerId);
        return ApiResponse.ok(page, "게시글별 댓글 목록");
    }

    /** 사용자별 댓글 목록 조회 */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> listByUser(
            @PathVariable UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            Pageable pageable
    ) {
        UUID viewerId = parseViewer(userHeader);
        Page<ForumPostCommentRes> page = queryService.listByUser(userId, pageable, viewerId);
        return ApiResponse.ok(page, "사용자별 댓글 목록");
    }

    /** 댓글 단건 조회 */
    @GetMapping("/{commentId}")
    public ResponseEntity<?> get(
            @PathVariable UUID commentId,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader
    ) {
        UUID viewerId = parseViewer(userHeader);
        ForumPostCommentRes dto = queryService.get(commentId, viewerId);
        return ApiResponse.ok(dto, "댓글 상세");
    }
}
