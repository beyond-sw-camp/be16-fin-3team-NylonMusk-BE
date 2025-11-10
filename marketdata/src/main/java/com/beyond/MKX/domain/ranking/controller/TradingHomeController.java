package com.beyond.MKX.domain.ranking.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.ranking.dto.TradingHomeItemResDTO;
import com.beyond.MKX.domain.ranking.dto.TradingItemDetailResDTO;
import com.beyond.MKX.domain.ranking.service.TradingHomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/trading-home")
@RequiredArgsConstructor
public class TradingHomeController {

    private final TradingHomeService tradingHomeService;

    // 홈 화면 리스트 (거래대금 상위)
    @GetMapping("/ranking")
    public ResponseEntity<?> getRankingTradeList(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<TradingHomeItemResDTO> resDTOList = tradingHomeService.getTopByTurnoverWithMeta(limit, ZoneId.systemDefault());
        return ApiResponse.ok(resDTOList);
    }

    // 종목의 현재가, 동락률 조회
    @PostMapping("/ranking/detail")
    public ResponseEntity<?> getTradeDetail(@RequestBody List<String> tickerList) {
        List<TradingItemDetailResDTO> resDTOList = tradingHomeService.getTickerDetail(tickerList);
        return ApiResponse.ok(resDTOList);
    }

}
