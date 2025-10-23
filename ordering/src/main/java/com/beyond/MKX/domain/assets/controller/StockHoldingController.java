package com.beyond.MKX.domain.assets.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.assets.dto.AccountIdResDTO;
import com.beyond.MKX.domain.assets.dto.StockHoldingResDTO;
import com.beyond.MKX.domain.assets.dto.StockUpdateDTO;
import com.beyond.MKX.domain.assets.service.StockHoldingService;
import com.beyond.MKX.domain.assets.service.StockUpdateApplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/my-stocks")
public class StockHoldingController {
    private final StockHoldingService stockHoldingService;
    private final StockUpdateApplyService applyService;

    // 1) 해당 계좌의 전체 보유 목록
    @GetMapping("/{memberAccountId}")
    public ResponseEntity<?> getMyStocks(@PathVariable UUID memberAccountId) {
        List<StockHoldingResDTO> result = stockHoldingService.getMyStocks(memberAccountId);
        return ApiResponse.ok(result, "전체 보유주식 조회입니다.");
    }

    // 2) 해당 계좌의 특정 종목 단건
    @GetMapping("/{memberAccountId}/{ticker}")
    public ResponseEntity<?> getMyStock(@PathVariable UUID memberAccountId, @PathVariable String ticker) {
        StockHoldingResDTO result = stockHoldingService.getMyStock(memberAccountId, ticker);
        return ApiResponse.ok(result, ticker + " 번 주식입니다.");
    }

    @PutMapping("/stock/update")
    public ResponseEntity<?> apply(@Validated @RequestBody StockUpdateDTO dto) {
        System.out.println("heeer");
        applyService.apply(dto);
        return ApiResponse.ok(null, "보유주식이 갱신되었습니다.");
    }

    @GetMapping("/{corpId}/account-brief")
    public ResponseEntity<?> getCorporationAccountId(@PathVariable UUID corpId) {
        AccountIdResDTO result = stockHoldingService.getCorporationAccountId(corpId);
        return ApiResponse.ok(result, "기업 계좌 ID 조회입니다.");
    }
}