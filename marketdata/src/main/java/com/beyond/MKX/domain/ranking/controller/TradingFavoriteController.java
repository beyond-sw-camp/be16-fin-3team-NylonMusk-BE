package com.beyond.MKX.domain.ranking.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.ranking.dto.MarketStockListResDTO;
import com.beyond.MKX.domain.ranking.service.MarketRankReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/market/my-favorites")
@RequiredArgsConstructor
public class TradingFavoriteController {

    private final MarketRankReaderService marketRankReaderService;

    /**
     * 관심종목 마켓 데이터 조회
     *
     * 사용자의 즐겨찾기 종목에 대한 마켓 데이터 반환
     * - 종목 정보: id, ticker, nameKo, status, delistingStage, imageUrl
     * - 마켓 데이터: currentPrice, changeRate, tradingVolume, marketCap
     *
     * @param userId API Gateway에서 전달하는 사용자 ID (헤더: X-User-Id)
     * @return 관심종목 마켓 데이터 리스트
     */
    @GetMapping
    public ResponseEntity<?> getMyFavoriteStocks(
            @RequestHeader("X-User-Id") UUID userId) {
        try {
            List<MarketStockListResDTO> favoriteStocks =
                    marketRankReaderService.getMyFavoriteStocks(userId);

            log.info("[API] 관심종목 마켓 데이터 조회 - userId:{}, {} 건",
                    userId, favoriteStocks.size());

            return ApiResponse.ok(favoriteStocks, "관심종목 마켓 데이터 조회 성공");

        } catch (Exception e) {
            log.error("[API] 관심종목 마켓 데이터 조회 실패 - userId:{}", userId, e);
            return ApiResponse.noContent("관심종목 마켓 데이터 조회 실패: " + e.getMessage());
        }
    }

}
