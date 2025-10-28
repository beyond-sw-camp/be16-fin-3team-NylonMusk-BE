package com.beyond.MKX.domain.delisting.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.delisting.dto.DelistingCriteriaCreateReqDto;
import com.beyond.MKX.domain.delisting.dto.DelistingCriteriaResDto;
import com.beyond.MKX.domain.delisting.dto.DelistingCriteriaUpdateReqDto;
import com.beyond.MKX.domain.delisting.service.command.DelistingCriteriaCommandService;
import com.beyond.MKX.domain.delisting.service.query.DelistingCriteriaQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delisting/criteria")
@RequiredArgsConstructor
public class DelistingCriteriaController {

    private final DelistingCriteriaCommandService commandService;
    private final DelistingCriteriaQueryService queryService;

    /** 생성 */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DelistingCriteriaCreateReqDto req) {
        DelistingCriteriaResDto res = commandService.create(req);
        return ApiResponse.created(res, "상장폐지 기준 생성 성공");
    }

    /** 수정 */
    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id,
                                    @Valid @RequestBody DelistingCriteriaUpdateReqDto req) {
        DelistingCriteriaResDto res = commandService.update(id, req);
        return ApiResponse.ok(res, "상장폐지 기준 수정 성공");
    }

    /** 소프트 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        commandService.softDelete(id);
        return ApiResponse.noContent(null, "상장폐지 기준 삭제(소프트) 성공");
    }

    /** 삭제 복구 */
    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable UUID id) {
        DelistingCriteriaResDto res = commandService.restore(id);
        return ApiResponse.ok(res, "상장폐지 기준 복구 성공");
    }

    /** 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        DelistingCriteriaResDto res = queryService.get(id);
        return ApiResponse.ok(res, "상장폐지 기준 조회 성공");
    }

    /** 목록 조회 */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue = "exclude") String deleted) {
        List<DelistingCriteriaResDto> res = queryService.list(deleted);
        return ApiResponse.ok(res, "상장폐지 기준 목록 조회 성공");
    }

    /** 유형별 조회 */
    @GetMapping("/type/{type}")
    public ResponseEntity<?> listByType(@PathVariable com.beyond.MKX.domain.delisting.entity.CriteriaType type) {
        List<DelistingCriteriaResDto> res = queryService.listByType(type);
        return ApiResponse.ok(res, "상장폐지 기준 유형별 조회 성공");
    }
}
