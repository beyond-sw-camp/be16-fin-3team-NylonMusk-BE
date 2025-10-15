package com.beyond.MKX.domain.forumCategory.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryCreateReqDto;
import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryResDto;
import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryUpdateReqDto;
import com.beyond.MKX.domain.forumCategory.service.command.ForumCategoryCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/forum/categories")
@RequiredArgsConstructor
public class ForumCategoryCommandController {

    private final ForumCategoryCommandService commandService;

    /** 생성 */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ForumCategoryCreateReqDto req) {
        ForumCategoryResDto res = commandService.create(req);
        return ApiResponse.created(res, "포럼 카테고리 생성 성공");
    }

    /** 수정 */
    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id,
                                    @Valid @RequestBody ForumCategoryUpdateReqDto req) {
        ForumCategoryResDto res = commandService.update(id, req);
        return ApiResponse.ok(res, "포럼 카테고리 수정 성공");
    }

    /** 소프트 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        commandService.softDelete(id);
        return ApiResponse.noContent(null, "포럼 카테고리 삭제(소프트) 성공");
    }

    /** 삭제 복구 */
    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable UUID id) {
        ForumCategoryResDto res = commandService.restore(id);
        return ApiResponse.ok(res, "포럼 카테고리 복구 성공");
    }
}
