package com.beyond.MKX.domain.assets.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.assets.dto.StockHoldingResDTO;
import com.beyond.MKX.domain.assets.service.StockHoldingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/my-stocks")
public class StockHoldingController {
    private final StockHoldingService stockHoldingService;

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
    }}
