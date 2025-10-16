// com/beyond/MKX/domain/forumPost/controller/ForumPostAdminController.java
package com.beyond.MKX.domain.forumPost.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import com.beyond.MKX.domain.forumPost.service.admin.ForumPostAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/forum/posts")
@RequiredArgsConstructor
public class ForumPostAdminController {

    private final ForumPostAdminService adminService;

    private static boolean isAdmin(String roleHeader) {
        return StringUtils.hasText(roleHeader) && roleHeader.toUpperCase().contains("EXCHANGE");
    }

    @GetMapping
    public ResponseEntity<?> listAllIncludingDeleted(@RequestHeader("X-User-Role") String roleHeader,
                                                     Pageable pageable) {
        if (!isAdmin(roleHeader)) return ApiResponse.forbidden("관리자 권한이 필요합니다.");
        Page<ForumPostResDto> page = adminService.listAllIncludingDeleted(pageable);
        return ApiResponse.ok(page, "게시글(삭제 포함) 목록 조회 성공");
    }

    @GetMapping("/deleted")
    public ResponseEntity<?> listDeleted(@RequestHeader("X-User-Role") String roleHeader,
                                         Pageable pageable) {
        if (!isAdmin(roleHeader)) return ApiResponse.forbidden("관리자 권한이 필요합니다.");
        Page<ForumPostResDto> page = adminService.listDeleted(pageable);
        return ApiResponse.ok(page, "삭제된 게시글 목록 조회 성공");
    }

    @GetMapping("/{postId}")
    public ResponseEntity<?> getIncludingDeleted(@RequestHeader("X-User-Role") String roleHeader,
                                                 @PathVariable UUID postId) {
        if (!isAdmin(roleHeader)) return ApiResponse.forbidden("관리자 권한이 필요합니다.");
        ForumPostResDto res = adminService.getIncludingDeleted(postId);
        return ApiResponse.ok(res, "게시글(삭제 포함) 조회 성공");
    }

    @PatchMapping("/{postId}/restore")
    public ResponseEntity<?> restore(@RequestHeader("X-User-Role") String roleHeader,
                                     @PathVariable UUID postId) {
        if (!isAdmin(roleHeader)) return ApiResponse.forbidden("관리자 권한이 필요합니다.");
        adminService.restore(postId);
        // TODO : 삭제 되지 않은 게시물도 게시물 복구 성공 처리로 반환되는 현상 수정 필요. 큰 이슈는 아니기에 후순위.
        return ApiResponse.ok(null, "게시글 복구 성공");
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> hardDelete(@RequestHeader("X-User-Role") String roleHeader,
                                        @PathVariable UUID postId) {
        if (!isAdmin(roleHeader)) return ApiResponse.forbidden("관리자 권한이 필요합니다.");
        adminService.hardDelete(postId);
        return ApiResponse.ok(null, "게시글 완전 삭제 성공");
    }
}
