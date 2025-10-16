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
@RequestMapping("/api/public/forum/posts")
@RequiredArgsConstructor
public class ForumPostPublicController {

    private final ForumPostQueryService forumPostQueryService;

    /** 글 목록: status 필터(optional), pageable */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) PostStatus status,
                                  Pageable pageable) {
        Page<ForumPostResDto> page = forumPostQueryService.list(status, pageable);
        return ApiResponse.ok(page, "게시글 목록 조회 성공");
    }

    /** 특정 유저 글 목록 */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<?> listByUser(@PathVariable UUID userId,
                                        @RequestParam(required = false) PostStatus status,
                                        Pageable pageable) {
        Page<ForumPostResDto> page = forumPostQueryService.listByUser(userId, status, pageable);
        return ApiResponse.ok(page, "사용자 게시글 조회 성공");
    }

    /** 특정 글 조회 */
    @GetMapping("/{postId}")
    public ResponseEntity<?> get(@PathVariable UUID postId) {
        ForumPostResDto res = forumPostQueryService.get(postId);
        return ApiResponse.ok(res, "게시글 조회 성공");
    }
}
