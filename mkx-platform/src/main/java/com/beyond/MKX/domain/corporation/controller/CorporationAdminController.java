package com.beyond.MKX.domain.corporation.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.ExchangeOrBrokerage;
import com.beyond.MKX.domain.corporation.service.CorporationAdminQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/corporations")
@RequiredArgsConstructor
public class CorporationAdminController {

    private final CorporationAdminQueryService corporationAdminQueryService;

    @ExchangeOrBrokerage
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<?> page = corporationAdminQueryService.list(q, status, pageable);
        return ApiResponse.ok(page, "기업 목록 조회");
    }

    @ExchangeOrBrokerage
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable("id") UUID id) {
        return ApiResponse.ok(corporationAdminQueryService.detail(id), "기업 상세 조회");
    }
}

