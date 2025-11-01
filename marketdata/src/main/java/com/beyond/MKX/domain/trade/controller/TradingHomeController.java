package com.beyond.MKX.domain.trade.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.trade.dto.TradingHomeItemResDTO;
import com.beyond.MKX.domain.trade.service.TradingHomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/trading/home")
@RequiredArgsConstructor
public class TradingHomeController {

    private final TradingHomeService tradingHomeService;

    // 홈 화면 리스트 (거래대금 상위)
    @GetMapping("/ranking")
    public ResponseEntity<?> getRanking(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "Asia/Seoul") String zone
    ) {
        List<TradingHomeItemResDTO> resDTOList = tradingHomeService.getTopByTurnoverWithMeta(limit, ZoneId.of(zone));
        return ApiResponse.ok(resDTOList);
    }
}
