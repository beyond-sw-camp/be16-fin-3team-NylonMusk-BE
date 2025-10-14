package com.beyond.MKX.domain.forumPost.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import com.beyond.MKX.domain.forumPost.entity.PostStatus;
import com.beyond.MKX.domain.forumPost.service.query.ForumPostQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/forum/posts")
@RequiredArgsConstructor
public class ForumPostQueryController {

    private final ForumPostQueryService service;

    /** 글 목록: status 필터(optional), pageable */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) PostStatus status,
                                  Pageable pageable) {
        Page<ForumPostResDto> page = service.list(status, pageable);
        return ApiResponse.ok(page, "게시글 목록 조회 성공");
    }

    /** 내 글 목록: 헤더 X-User-Id 사용 */
    @GetMapping("/me")
    public ResponseEntity<?> listMine(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
                                      @RequestParam(required = false) PostStatus status,
                                      Pageable pageable) {
        if (!StringUtils.hasText(userIdHeader)) {
            return ApiResponse.ok(null, "인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }
        Page<ForumPostResDto> page = service.listMine(UUID.fromString(userIdHeader), status, pageable);
        return ApiResponse.ok(page, "내 게시글 조회 성공");
    }

    /** 특정 유저 글 목록 */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<?> listByUser(@PathVariable UUID userId,
                                        @RequestParam(required = false) PostStatus status,
                                        Pageable pageable) {
        Page<ForumPostResDto> page = service.listByUser(userId, status, pageable);
        return ApiResponse.ok(page, "사용자 게시글 조회 성공");
    }
}
