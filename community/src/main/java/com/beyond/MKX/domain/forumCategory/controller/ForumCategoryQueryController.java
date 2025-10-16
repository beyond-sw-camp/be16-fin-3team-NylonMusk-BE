package com.beyond.MKX.domain.forumCategory.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryResDto;
import com.beyond.MKX.domain.forumCategory.service.query.ForumCategoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/forum/categories")
@RequiredArgsConstructor
public class ForumCategoryQueryController {

    private final ForumCategoryQueryService queryService;

    /** 단건 조회 (삭제 포함) */
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        ForumCategoryResDto res = queryService.get(id);
        return ApiResponse.ok(res, "포럼 카테고리 조회 성공");
    }

    /** 전체 조회 (?deleted=all|only|exclude) - 기본 exclude(미삭제) */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue = "exclude") String deleted,
                                  Pageable pageable) {
        Page<ForumCategoryResDto> page = queryService.list(deleted, pageable);
        return ApiResponse.ok(page, "포럼 카테고리 목록 조회 성공");
    }

    /** 삭제된 것만 */
    @GetMapping("/deleted")
    public ResponseEntity<?> listDeleted(Pageable pageable) {
        return ApiResponse.ok(queryService.list("only", pageable), "삭제된 카테고리 목록 조회 성공");
    }

    /** 미삭제(활성)만 */
    @GetMapping("/active")
    public ResponseEntity<?> listActive(Pageable pageable) {
        return ApiResponse.ok(queryService.list("exclude", pageable), "활성 카테고리 목록 조회 성공");
    }

    /** 특정 사용자 기준 조회 (uuid 또는 userId 둘 중 하나 제공) */
    @GetMapping("/by-user")
    public ResponseEntity<?> listByUser(@RequestParam(required = false) UUID userUuid,
                                        @RequestParam(required = false) String userId,
                                        @RequestParam(defaultValue = "exclude") String deleted,
                                        Pageable pageable) {
        Page<ForumCategoryResDto> page = queryService.listByUser(userUuid, userId, deleted, pageable);
        return ApiResponse.ok(page, "사용자 기준 카테고리 조회 성공");
    }
}
