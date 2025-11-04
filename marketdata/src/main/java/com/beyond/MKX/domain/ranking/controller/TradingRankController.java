package com.beyond.MKX.domain.ranking.controller;


import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.apiResponse.CommonDTO;
import com.beyond.MKX.domain.ranking.dto.TradingRankDTO;
import com.beyond.MKX.domain.ranking.service.TradingRankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.shaded.com.google.protobuf.Api;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 거래대금/거래량 랭킹 컨트롤러
 *
 * Redis Sorted Set(Z-Set) 기반 실시간 랭킹 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/market/rank")
@RequiredArgsConstructor
public class TradingRankController {

    private final TradingRankService tradingRankService;

    /**
     * 거래대금 TOP 30 조회
     *
     * @return 거래대금 TOP 30 리스트
     */
    @GetMapping("/trading-value/top30")
    public ResponseEntity<?> getTop30ByTradingValue() {
        try {
            List<TradingRankDTO> rankList = tradingRankService.getTop30ByTradingValue();

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
            List<TradingRankDTO> rankList = tradingRankService.getTop30ByTradingVolume();

            log.info("[API] 거래량 TOP 30 조회 - {} 건", rankList.size());
            return ApiResponse.ok(rankList, "거래량 TOP 30 조회 성공");

        } catch (Exception e) {
            log.error("[API] 거래량 TOP 30 조회 실패", e);
            return ApiResponse.noContent("거래량 TOP 30 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 특정 종목의 랭킹 정보 조회
     *
     * @param ticker 종목 코드
     * @return 랭킹 정보
     */
    @GetMapping("/{ticker}")
    public ResponseEntity<?> getRankByTicker(@PathVariable String ticker) {
        try {
            TradingRankDTO rank = tradingRankService.getRankByTicker(ticker);

            if (rank == null) {
                return ApiResponse.noContent("종목 " + ticker + "의 랭킹 정보를 찾을 수 없습니다.");
            }

            log.info("[API] 종목 {} 랭킹 조회 - 거래대금: {}위, 거래량: {}위",
                    ticker, rank.getValueRank(), rank.getVolumeRank());

            return ApiResponse.ok(rank, "종목 랭킹 조회 성공");

        } catch (Exception e) {
            log.error("[API] 종목 {} 랭킹 조회 실패", ticker, e);
            return ApiResponse.noContent("종목 랭킹 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 랭킹 수동 업데이트 (관리자용)
     *
     * @return 업데이트 결과
     */
    @PostMapping("/update")
    public ResponseEntity<?> updateRanks() {
        try {
            tradingRankService.updateTradingRanks();

            log.info("[API] 거래대금/거래량 랭킹 수동 업데이트 완료");
            return ApiResponse.ok("랭킹 업데이트 성공");

        } catch (Exception e) {
            log.error("[API] 랭킹 수동 업데이트 실패", e);
            return ApiResponse.noContent("랭킹 업데이트 실패: " + e.getMessage());
        }
    }
}
