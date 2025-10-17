package com.beyond.MKX.domain.forumVote.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.forumVote.dto.ForumVoteResDto;
import com.beyond.MKX.domain.forumVote.service.admin.ForumVoteAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/forum/votes")
public class ForumVoteAdminController {

    private final ForumVoteAdminService adminService;

    @GetMapping
    public ResponseEntity<?> listAll(Pageable pageable) {
        Page<ForumVoteResDto> page = adminService.listAll(pageable);
        return ApiResponse.ok(page, "전체 투표 목록 조회 성공");
    }
}
