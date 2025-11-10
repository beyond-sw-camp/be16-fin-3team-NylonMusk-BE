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
        tradingBotService.createSimpleBots(ticker);
        return ApiResponse.ok(null, ticker + " 종목의 간단한 매수/매도 봇이 생성되었습니다!");
    }

    /** 특정 봇의 상태 조회 (인증 없이 접근 가능) */
    @GetMapping("/test/config/{id}/status")
    public ResponseEntity<?> getBotStatus(@PathVariable UUID id) {
        TradingBotConfigDTO res = tradingBotService.getBotConfig(id);
        return ApiResponse.ok(res, "트레이딩 봇 상태 조회 성공");
    }

    /** 거래량 데이터 쌓기용 봇 생성 (25초마다 한 번씩, 가격 변동 엄청 크게) */
    @PostMapping("/test/volume-bot/{ticker}")
    public ResponseEntity<?> createVolumeBuildingBot(@PathVariable String ticker,
                                                      @RequestParam(required = false) UUID buyAccountId,
                                                      @RequestParam(required = false) UUID sellAccountId) {
        tradingBotService.createVolumeBuildingBot(ticker, buyAccountId, sellAccountId);
        return ApiResponse.ok(null, ticker + " 종목의 거래량 데이터 쌓기용 봇이 생성되었습니다! (25초마다 주문, 가격 변동 5~15%)");
    }
}
