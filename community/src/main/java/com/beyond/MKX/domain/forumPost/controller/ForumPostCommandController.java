package com.beyond.MKX.domain.forumPost.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.forumPost.dto.ForumPostCreateReq;
import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import com.beyond.MKX.domain.forumPost.dto.ForumPostStatusUpdateReq;
import com.beyond.MKX.domain.forumPost.dto.ForumPostUpdateReq;
import com.beyond.MKX.domain.forumPost.service.command.ForumPostCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/forum/posts")
@RequiredArgsConstructor
public class ForumPostCommandController {

    private final ForumPostCommandService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestHeader("X-User-Id") UUID actorId,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @Valid @ModelAttribute ForumPostCreateReq req
    ) {
        ForumPostResDto res = service.create(actorId, roleHeader, req);
        URI location = URI.create("/api/forum/posts/" + res.id());
        // ApiResponse.created(location, ...) 헬퍼가 없으면 그냥 created 사용 or ok 로 통일
        return ResponseEntity.created(location).body(ApiResponse.created(res, "게시글 작성 성공").getBody());
    }

    /** 글 수정 (작성자만) */
    @PatchMapping("/{postId}")
    public ResponseEntity<?> update(@PathVariable UUID postId,
                                    @RequestHeader("X-User-Id") UUID actorId,
                                    @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                    @Valid @RequestBody ForumPostUpdateReq req) {
        ForumPostResDto res = service.update(postId, actorId, roleHeader, req);
        return ApiResponse.ok(res, "게시글 수정 성공");
    }

    /** 글 삭제 (작성자 또는 관리자) */
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> delete(@PathVariable UUID postId,
                                    @RequestHeader("X-User-Id") UUID actorId,
                                    @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        service.delete(postId, actorId, roleHeader);
        return ApiResponse.noContent(null, "게시글 삭제(소프트) 성공");
    }

    /** 글 상태 수정 (관리자만) */
    @PatchMapping("/{postId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID postId,
                                          @RequestHeader("X-User-Role") String roleHeader,
                                          @Valid @RequestBody ForumPostStatusUpdateReq req) {
        ForumPostResDto res = service.updateStatus(postId, roleHeader, req);
        return ApiResponse.ok(res, "게시글 상태 변경 성공");
    }
}
