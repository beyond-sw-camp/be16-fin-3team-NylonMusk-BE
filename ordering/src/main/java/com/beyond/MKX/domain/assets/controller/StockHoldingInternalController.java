package com.beyond.MKX.domain.assets.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.assets.dto.StockHoldingResDTO;
import com.beyond.MKX.domain.assets.service.StockHoldingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 주식 보유 내부 API 컨트롤러
 * 다른 서비스(mkx-platform)에서 호출하는 내부 API
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/stock-holdings")
public class StockHoldingInternalController {
    
    private final StockHoldingService stockHoldingService;

    /**
     * 특정 ticker의 모든 보유자 조회 (상장폐지 보상금 계산용)
     * 
     * @param ticker 주식 티커
     * @return 해당 주식을 보유한 모든 계좌 정보
     */
    @GetMapping("/ticker/{ticker}")
    public ResponseEntity<?> getAllHoldersByTicker(@PathVariable String ticker) {
        List<StockHoldingResDTO> result = stockHoldingService.getAllHoldersByTicker(ticker);
        return ApiResponse.ok(result, ticker + " 보유자 목록 조회 성공");
    }

    /**
     * 특정 ticker의 모든 stock holdings 삭제 (상장폐지용)
     * 
     * @param ticker 주식 티커
     * @return 삭제된 개수
     */
    @DeleteMapping("/ticker/{ticker}")
    public ResponseEntity<?> deleteAllByTicker(@PathVariable String ticker) {
        int deletedCount = stockHoldingService.deleteAllByTicker(ticker);
        return ApiResponse.ok(deletedCount, ticker + " 보유 정보 " + deletedCount + "건 삭제 완료");
    }
}
