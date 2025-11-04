package com.beyond.MKX.domain.stock.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.stock.dto.StockBriefDTO;
import com.beyond.MKX.domain.stock.dto.StockDetailResDTO;
import com.beyond.MKX.domain.stock.dto.StockListResDto;
import com.beyond.MKX.domain.stock.dto.StockInfoResDTO;
import com.beyond.MKX.domain.stock.dto.StockPriceRatiosResDTO;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import com.beyond.MKX.domain.stock.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Validated
public class StockController {

    private final StockQueryService stockQueryService;

    /**
     * GET /api/stocks?page=0&size=20&sort=ticker,asc&status=LISTED&q=검색어
     */
    @GetMapping
    public Page<StockListResDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "ticker", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return stockQueryService.getStocks(q, status, pageable);
    }

    @GetMapping("/{ticker}")
    public StockInfoResDTO getStockByTicker(@PathVariable String ticker) {
        return stockQueryService.getStockByTicker(ticker);
    }

    /**
     * 종목 상세 정보 조회 (Corporation 정보 포함)
     * GET /api/stocks/{ticker}/detail
     */
    @GetMapping("/{ticker}/detail")
    public ResponseEntity<?> getStockDetail(@PathVariable String ticker) {
        StockDetailResDTO stockDetail = stockQueryService.getStockDetail(ticker);
        return ApiResponse.ok(stockDetail, "종목 상세 정보 조회 성공");
    }

    /**
     * 종목의 현재가 기반 재무비율 조회 (PER, PBR, PSR, 시가총액, Enterprise Value)
     * GET /api/stocks/{ticker}/price-ratios
     */
    @GetMapping("/{ticker}/price-ratios")
    public ResponseEntity<?> getStockPriceRatios(@PathVariable String ticker) {
        StockPriceRatiosResDTO priceRatios = stockQueryService.getStockPriceRatios(ticker);
        if (priceRatios == null) {
            return ApiResponse.ok(null, "현재가 기반 비율 데이터가 없습니다.");
        }
        return ApiResponse.ok(priceRatios, "현재가 기반 재무비율 조회 성공");
    }
}

@RestController
@RequestMapping("/api/internal/stocks")
@RequiredArgsConstructor
@Validated
class StockInternalController {

    private final StockQueryService stockQueryService;
    private final StockRepository stockRepository;


    @GetMapping("/{ticker}")
    public StockInfoResDTO getStockByTicker(@PathVariable String ticker) {
        return stockQueryService.getStockByTicker(ticker);
    }


    // 종목 간략 정보 조회 (이름 + id + status + delistingStage)
    @GetMapping("/briefs")
    public List<StockBriefDTO> getBriefs(@RequestParam("tickers") List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) return List.of();
        return stockRepository.findByTickerIn(tickers).stream()
                .map(v -> StockBriefDTO.builder()
                        .id(v.getId())
                        .ticker(v.getTicker())
                        .nameKo(v.getNameKo())
                        .status(v.getStatus())
                        .delistingStage(v.getDelistingStage())
                        .imageUrl(v.getImageUrl())
                        .build())
                .toList();
    }
}
