package com.beyond.MKX.domain.disclosure.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.ExchangeOnly;
import com.beyond.MKX.domain.disclosure.mapper.DisclosureMapper;
import com.beyond.MKX.domain.disclosure.service.DisclosureAdminService;
import com.beyond.MKX.domain.disclosure.service.DisclosureAdminQueryService;
import com.beyond.MKX.domain.disclosure.entity.Disclosure;
import com.beyond.MKX.domain.disclosure.dto.DisclosureRejectReqDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import com.beyond.MKX.domain.disclosure.dto.DisclosureResDto;
import com.beyond.MKX.domain.disclosure.entity.DisclosureStatus;
import com.beyond.MKX.domain.disclosure.entity.DisclosureType;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/disclosures")
@Validated
public class DisclosureAdminController {

    private final DisclosureAdminService disclosureAdminService;
    private final DisclosureAdminQueryService disclosureAdminQueryService;

    /** 공시 승인 (거래소 관리자) */
    @ExchangeOnly
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable UUID id) {
        Disclosure approved = disclosureAdminService.approve(id);
        return ApiResponse.ok(DisclosureMapper.toRes(approved), "공시 승인 완료");
    }

    /** 공시 반려 (거래소 관리자) */
    @ExchangeOnly
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable UUID id, @Valid @RequestBody DisclosureRejectReqDto request) {
        Disclosure rejected = disclosureAdminService.reject(id, request.getCode(), request.getReason());
        return ApiResponse.ok(DisclosureMapper.toRes(rejected), "공시 반려 완료");
    }

    /** 관리자 전용 공시 조회(상태/유형/종목/제목/기간 필터) */
    @ExchangeOnly
    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) DisclosureStatus status,
            @RequestParam(required = false) DisclosureType type,
            @RequestParam(required = false) UUID stockId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime toExclusive = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;
        Page<DisclosureResDto> page = disclosureAdminQueryService.search(status, type, stockId, title, from, toExclusive, pageable);
        return ApiResponse.ok(page, "관리자 공시 조회 완료");
    }

    /**
     * displayNo 기준 전체 리비전 이력 조회
     */
    @ExchangeOnly
    @GetMapping("/{displayNo}/revisions")
    public ResponseEntity<?> revisions(@PathVariable String displayNo) {
        List<DisclosureResDto> list = disclosureAdminQueryService.listRevisionsByDisplayNo(displayNo);
        return ApiResponse.ok(list, "정정 공시 이력 조회 완료");
    }
}
