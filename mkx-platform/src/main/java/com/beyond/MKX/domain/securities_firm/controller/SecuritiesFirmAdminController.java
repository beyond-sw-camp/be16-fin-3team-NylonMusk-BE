package com.beyond.MKX.domain.securities_firm.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.ExchangeOrBrokerage;
import com.beyond.MKX.domain.securities_firm.service.SecuritiesFirmAdminQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/securities-firms")
@RequiredArgsConstructor
public class SecuritiesFirmAdminController {

    private final SecuritiesFirmAdminQueryService queryService;

    @ExchangeOrBrokerage
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<?> page = queryService.list(q, status, pageable);
        return ApiResponse.ok(page, "증권사 목록 조회");
    }

    @ExchangeOrBrokerage
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable("id") UUID id) {
        return ApiResponse.ok(queryService.detail(id), "증권사 상세 조회");
    }
}

