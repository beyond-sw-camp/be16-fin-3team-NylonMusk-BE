package com.beyond.MKX.domain.forumVote.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.forumVote.dto.*;
import com.beyond.MKX.domain.forumVote.service.command.ForumVoteCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/forum/votes")
public class ForumVoteCommandController {

    private final ForumVoteCommandService commandService;

    private UUID requireUser(String header) {
        if (header == null || header.isBlank())
            throw new SecurityException("X-User-Id 헤더가 없습니다.");
        try { return UUID.fromString(header); }
        catch (IllegalArgumentException e) { throw new SecurityException("X-User-Id 형식이 올바르지 않습니다."); }
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ForumVoteCreateReqDto req) {
        ForumVoteResDto res = commandService.create(req);
        return ApiResponse.created(res, "투표 생성 성공");
    }

    /** 투표 제출/변경 */
    @PostMapping("/{voteId}/ballots")
    public ResponseEntity<?> cast(@RequestHeader("X-User-Id") String userHeader,
                                  @PathVariable UUID voteId,
                                  @Valid @RequestBody ForumVoteCastReqDto req) {
        var res = commandService.cast(voteId, requireUser(userHeader), req);
        return ApiResponse.ok(res, "투표가 반영되었습니다.");
    }

    /** 전체 철회 */
    @DeleteMapping("/{voteId}/ballots")
    public ResponseEntity<?> revoke(@RequestHeader("X-User-Id") String userHeader,
                                    @PathVariable UUID voteId) {
        var res = commandService.revoke(voteId, requireUser(userHeader));
        return ApiResponse.ok(res, "투표가 철회되었습니다.");
    }

    @PatchMapping("/{voteId}/selections/reorder")
    public ResponseEntity<?> reorder(@PathVariable UUID voteId,
                                     @RequestHeader("X-User-Id") String userHeader,
                                     @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                     @RequestBody ForumVoteReorderReqDto req) {
        UUID actorId = requireUser(userHeader); // 기존 requireUser 재사용 (UUID 파싱/검증)
        commandService.reorderSelections(voteId, actorId, roleHeader, req.selectionIdsInOrder());
        return ApiResponse.ok(null, "선택지 순서가 변경되었습니다.");
    }

    public record ForumVoteReorderReqDto(List<UUID> selectionIdsInOrder) {}
}
