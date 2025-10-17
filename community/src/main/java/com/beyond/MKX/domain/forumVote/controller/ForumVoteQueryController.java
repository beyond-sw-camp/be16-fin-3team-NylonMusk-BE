package com.beyond.MKX.domain.forumVote.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.forumVote.dto.ForumVoteResDto;
import com.beyond.MKX.domain.forumVote.service.query.ForumVoteQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/forum/votes")
public class ForumVoteQueryController {

    private final ForumVoteQueryService queryService;

    private UUID parseViewer(String header) {
        if (header == null || header.isBlank()) return null;
        try { return UUID.fromString(header); } catch (Exception e) { return null; }
    }

    @GetMapping("/{voteId}")
    public ResponseEntity<?> get(@PathVariable UUID voteId,
                                 @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
        UUID viewer = parseViewer(userHeader);
        ForumVoteResDto res = queryService.get(voteId, viewer);
        return ApiResponse.ok(res, "투표 조회 성공");
    }

    @GetMapping("/by-post/{postId}")
    public ResponseEntity<?> getByPost(@PathVariable UUID postId,
                                       @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
        UUID viewer = parseViewer(userHeader);
        ForumVoteResDto res = queryService.getByPost(postId, viewer);
        return ApiResponse.ok(res, "게시글 투표 조회 성공");
    }
}
