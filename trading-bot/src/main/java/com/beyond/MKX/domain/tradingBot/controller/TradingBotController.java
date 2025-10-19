package com.beyond.MKX.domain.tradingBot.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.tradingBot.dto.CreateTradingBotConfigRequest;
import com.beyond.MKX.domain.tradingBot.dto.TradingBotConfigDTO;
import com.beyond.MKX.domain.tradingBot.dto.UpdateTradingBotStatusRequest;
import com.beyond.MKX.domain.tradingBot.service.TradingBotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/test/api/trading-bot")
@RequiredArgsConstructor
public class TradingBotController {

    private final TradingBotService tradingBotService;

    /** 봇 설정 생성 */
    @PostMapping("/config")
    public ResponseEntity<?> create(@Valid @RequestBody CreateTradingBotConfigRequest req) {
        TradingBotConfigDTO res = tradingBotService.createBotConfig(req);
        return ApiResponse.created(res, "트레이딩 봇 설정 생성 성공");
    }

    /** 봇 상태 업데이트 */
    @PatchMapping("/config/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id,
                                          @Valid @RequestBody UpdateTradingBotStatusRequest req) {
        TradingBotConfigDTO res = tradingBotService.updateBotStatus(id, req);
        return ApiResponse.ok(res, "트레이딩 봇 상태 변경 성공");
    }

    /** 봇 비활성화 */
    @DeleteMapping("/config/{id}")
    public ResponseEntity<?> deactivate(@PathVariable UUID id) {
        tradingBotService.deactivateBot(id);
        return ApiResponse.noContent(null, "트레이딩 봇 비활성화 성공");
    }

    /** 활성화된 모든 봇 설정 조회 */
    @GetMapping("/configs/active")
    public ResponseEntity<?> listActive() {
        List<TradingBotConfigDTO> res = tradingBotService.getAllActiveBotConfigs();
        return ApiResponse.ok(res, "활성화된 트레이딩 봇 설정 조회 성공");
    }

    /** 특정 봇 설정 조회 */
    @GetMapping("/config/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        TradingBotConfigDTO res = tradingBotService.getBotConfig(id);
        return ApiResponse.ok(res, "트레이딩 봇 설정 조회 성공");
    }

    /** 상태별 봇 설정 조회 */
    @GetMapping("/configs/status/{status}")
    public ResponseEntity<?> listByStatus(@PathVariable String status) {
        List<TradingBotConfigDTO> res = tradingBotService.getAllActiveBotConfigs()
                .stream()
                .filter(c -> status.equals(c.getStatus()))
                .toList();
        return ApiResponse.ok(res, "트레이딩 봇 상태별 조회 성공");
    }

    /** 다이나믹 봇 생성 - 자동으로 매수/매도 봇들을 생성하여 횡보 시뮬레이션 */
    @PostMapping("/dynamic/{ticker}")
    public ResponseEntity<?> createDynamicBots(@PathVariable String ticker) {
        tradingBotService.createDynamicBots(ticker);
        return ApiResponse.ok(null, ticker + " 종목의 다이나믹 봇들이 생성되었습니다!");
    }

    /** 모든 봇 비활성화 */
    @DeleteMapping("/configs/all")
    public ResponseEntity<?> deactivateAllBots() {
        List<TradingBotConfigDTO> activeBots = tradingBotService.getAllActiveBotConfigs();
        activeBots.forEach(bot -> tradingBotService.deactivateBot(bot.getId()));
        return ApiResponse.ok(null, "모든 봇이 비활성화되었습니다!");
    }

    /** 테스트용 헬스체크 (인증 없이 접근 가능) */
    @GetMapping("/test/health")
    public ResponseEntity<?> healthCheck() {
        return ApiResponse.ok("OK", "Trading Bot Service is running!");
    }

    /** 테스트용 다이나믹 봇 생성 (인증 없이 접근 가능) */
    @PostMapping("/test/dynamic/{ticker}")
    public ResponseEntity<?> createDynamicBotsTest(@PathVariable String ticker) {
        tradingBotService.createDynamicBots(ticker);
        return ApiResponse.ok(null, ticker + " 종목의 다이나믹 봇들이 생성되었습니다!");
    }

    /** 테스트용 봇 목록 조회 (인증 없이 접근 가능) */
    @GetMapping("/test/configs/active")
    public ResponseEntity<?> listActiveTest() {
        List<TradingBotConfigDTO> res = tradingBotService.getAllActiveBotConfigs();
        return ApiResponse.ok(res, "활성화된 트레이딩 봇 설정 조회 성공");
    }

    /** 테스트용 간단한 봇 생성 (인증 없이 접근 가능) */
    @PostMapping("/test/simple-bot")
    public ResponseEntity<?> createSimpleBot(@RequestParam String ticker) {
        // 기존 봇들 삭제
        List<TradingBotConfigDTO> activeBots = tradingBotService.getAllActiveBotConfigs();
        activeBots.forEach(bot -> tradingBotService.deactivateBot(bot.getId()));
        
        // 간단한 매수/매도 봇 생성
        long basePrice = getBasePrice(ticker);
        
        // 매수 봇 (기본가 -10% ~ 기본가)
        CreateTradingBotConfigRequest buyRequest = new CreateTradingBotConfigRequest();
        buyRequest.setTicker(ticker);
        buyRequest.setStatus("START");
        buyRequest.setPriceLimitHigh(basePrice);
        buyRequest.setPriceLimitLow((long)(basePrice * 0.9));
        buyRequest.setQuantity(BigDecimal.valueOf(10));
        buyRequest.setSide("BUY");
        buyRequest.setOrderType("LIMIT");
        buyRequest.setBrokerageId("SIMPLE_BROKER");
        buyRequest.setDescription("간단 매수 봇");
        tradingBotService.createBotConfig(buyRequest);
        
        // 매도 봇 (기본가 ~ 기본가 +10%)
        CreateTradingBotConfigRequest sellRequest = new CreateTradingBotConfigRequest();
        sellRequest.setTicker(ticker);
        sellRequest.setStatus("START");
        sellRequest.setPriceLimitHigh((long)(basePrice * 1.1));
        sellRequest.setPriceLimitLow(basePrice);
        sellRequest.setQuantity(BigDecimal.valueOf(10));
        sellRequest.setSide("SELL");
        sellRequest.setOrderType("LIMIT");
        sellRequest.setBrokerageId("SIMPLE_BROKER");
        sellRequest.setDescription("간단 매도 봇");
        tradingBotService.createBotConfig(sellRequest);
        
        return ApiResponse.ok(null, ticker + " 종목의 간단한 매수/매도 봇이 생성되었습니다!");
    }
    
    private long getBasePrice(String ticker) {
        return switch (ticker) {
            case "MKX001" -> 50000;
            case "MKX002" -> 35000;
            case "MKX003" -> 75000;
            case "MKX004" -> 25000;
            case "MKX005" -> 40000;
            case "MKX006" -> 120000;
            case "MKX007" -> 60000;
            case "MKX008" -> 85000;
            case "MKX009" -> 30000;
            case "MKX010" -> 95000;
            default -> 50000;
        };
    }
}
