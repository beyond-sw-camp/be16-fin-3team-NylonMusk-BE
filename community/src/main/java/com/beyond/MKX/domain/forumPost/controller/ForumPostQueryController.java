package com.beyond.MKX.domain.forumPost.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import com.beyond.MKX.domain.forumPost.entity.PostStatus;
import com.beyond.MKX.domain.forumPost.service.query.ForumPostQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/forum/posts")
@RequiredArgsConstructor
public class ForumPostQueryController {

    private final ForumPostQueryService forumPostQueryService;

    /** 내 글 목록: 헤더 X-User-Id 사용 */
    @GetMapping("/me")
    public ResponseEntity<?> listMine(@RequestHeader("X-User-Id") UUID actorId,
                                      @RequestParam(required = false) PostStatus status,
                                      Pageable pageable) {
        Page<ForumPostResDto> page = forumPostQueryService.listMine(actorId, status, pageable);
        return ApiResponse.ok(page, "내 게시글 조회 성공");
    }

    /** 게시글 상세 조회 (댓글, 투표 포함) */
    @GetMapping("/{postId}/details")
    public ResponseEntity<?> getWithDetails(@PathVariable UUID postId,
                                           @RequestHeader(value = "X-User-Id", required = false) UUID viewerId) {
        ForumPostResDto post = forumPostQueryService.getWithDetails(postId, viewerId);
        return ApiResponse.ok(post, "게시글 상세 조회 성공");
    }
}
