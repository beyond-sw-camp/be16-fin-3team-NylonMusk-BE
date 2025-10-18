package com.beyond.MKX.domain.disclosure.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.ExchangeOnly;
import com.beyond.MKX.domain.disclosure.mapper.DisclosureMapper;
import com.beyond.MKX.domain.disclosure.service.DisclosureAdminService;
import com.beyond.MKX.domain.disclosure.entity.Disclosure;
import com.beyond.MKX.domain.disclosure.dto.DisclosureRejectReqDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/disclosures")
@Validated
public class DisclosureAdminController {

    private final DisclosureAdminService disclosureAdminService;

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
}
