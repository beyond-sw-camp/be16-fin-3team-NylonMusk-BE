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
@RequestMapping("/api/forum/comments")
public class ForumPostCommentAuthenticatedController {

    private final ForumPostCommentQueryService queryService;

    /** 인증된 사용자용 게시글별 댓글 목록 조회 */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<?> listByPostAuthenticated(
            @PathVariable UUID postId,
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable
    ) {
        Page<ForumPostCommentRes> page = queryService.listByPost(postId, pageable, userId);
        return ApiResponse.ok(page, "인증된 사용자 댓글 목록");
    }

    /** 인증된 사용자용 사용자별 댓글 목록 조회 */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> listByUserAuthenticated(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") UUID viewerId,
            Pageable pageable
    ) {
        Page<ForumPostCommentRes> page = queryService.listByUser(userId, pageable, viewerId);
        return ApiResponse.ok(page, "인증된 사용자별 댓글 목록");
    }

    /** 인증된 사용자용 댓글 단건 조회 */
    @GetMapping("/{commentId}")
    public ResponseEntity<?> getAuthenticated(
            @PathVariable UUID commentId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        ForumPostCommentRes dto = queryService.get(commentId, userId);
        return ApiResponse.ok(dto, "인증된 사용자 댓글 상세");
    }
}
