package com.beyond.MKX.domain.ranking.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.ranking.dto.MarketStockListResDTO;
import com.beyond.MKX.domain.ranking.service.MarketRankReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 마켓 랭킹 컨트롤러
 *
 * Redis Sorted Set 기반 실시간 랭킹 제공
 * - 등락률 랭킹 (상승률/하락률)
 * - 거래대금 랭킹
 * - 거래량 랭킹
 */
@Slf4j
@RestController
@RequestMapping("/api/market/rank")
@RequiredArgsConstructor
public class TradingRankController {

    private final MarketRankReaderService marketRankReaderService;

    /**
     * 상승률 TOP 30 조회
     *
     * @return 상승률 TOP 30 리스트
     */
    @GetMapping("/change-rate/soar/top30")
    public ResponseEntity<?> getTop30BySoarChangeRate() {
        try {
            List<MarketStockListResDTO> rankList = marketRankReaderService.getTodayTop30BySoarChangeRate();

            log.info("[API] 상승률 TOP 30 조회 - {} 건", rankList.size());
            return ApiResponse.ok(rankList, "상승률 TOP 30 조회 성공");

        } catch (Exception e) {
            log.error("[API] 상승률 TOP 30 조회 실패", e);
            return ApiResponse.noContent("상승률 TOP 30 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 하락률 TOP 30 조회
     *
     * @return 하락률 TOP 30 리스트
     */
    @GetMapping("/change-rate/descent/top30")
    public ResponseEntity<?> getTop30ByDescentChangeRate() {
        try {
            List<MarketStockListResDTO> rankList = marketRankReaderService.getTodayTop30ByDescentChangeRate();

            log.info("[API] 하락률 TOP 30 조회 - {} 건", rankList.size());
            return ApiResponse.ok(rankList, "하락률 TOP 30 조회 성공");

        } catch (Exception e) {
            log.error("[API] 하락률 TOP 30 조회 실패", e);
            return ApiResponse.noContent("하락률 TOP 30 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 거래대금 TOP 30 조회
     *
     * @return 거래대금 TOP 30 리스트
     */
    @GetMapping("/trading-value/top30")
    public ResponseEntity<?> getTop30ByTradingValue() {
        try {
            List<MarketStockListResDTO> rankList = marketRankReaderService.getTodayTop30ByTradingValue();

            log.info("[API] 거래대금 TOP 30 조회 - {} 건", rankList.size());
            return ApiResponse.ok(rankList, "거래대금 TOP 30 조회 성공");

        } catch (Exception e) {
            log.error("[API] 거래대금 TOP 30 조회 실패", e);
            return ApiResponse.noContent("거래대금 TOP 30 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 거래량 TOP 30 조회
     *
     * @return 거래량 TOP 30 리스트
     */
    @GetMapping("/trading-volume/top30")
    public ResponseEntity<?> getTop30ByTradingVolume() {
        try {
            List<MarketStockListResDTO> rankList = marketRankReaderService.getTodayTop30ByTradingVolume();

            log.info("[API] 거래량 TOP 30 조회 - {} 건", rankList.size());
            return ApiResponse.ok(rankList, "거래량 TOP 30 조회 성공");

        } catch (Exception e) {
            log.error("[API] 거래량 TOP 30 조회 실패", e);
            return ApiResponse.noContent("거래량 TOP 30 조회 실패: " + e.getMessage());
        }
    }
}
