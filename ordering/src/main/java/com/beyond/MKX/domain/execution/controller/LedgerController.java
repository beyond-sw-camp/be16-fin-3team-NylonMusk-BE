package com.beyond.MKX.domain.execution.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.execution.dto.LedgerResponseDTO;
import com.beyond.MKX.domain.execution.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ledgers")
@RequiredArgsConstructor
public class LedgerController {
    private final LedgerService ledgerService;

    /**
     * 특정 계좌의 거래내역을 조회합니다.
     * 
     * @param memberAccountId 계좌 ID
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @param type 거래 유형 필터 (BUY, SELL, DEPOSIT, WITHDRAWAL 등)
     * @return 거래내역 페이지
     */
    @GetMapping("/my-account/{memberAccountId}")
    public ResponseEntity<?> getMyLedgers(
            @PathVariable UUID memberAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        
        Page<LedgerResponseDTO> ledgers = ledgerService.getMyLedgers(memberAccountId, page, size, type);
        return ApiResponse.ok(ledgers, "거래내역 조회 성공");
    }
}

