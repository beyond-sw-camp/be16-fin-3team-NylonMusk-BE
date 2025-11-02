package com.beyond.MKX.domain.stock.controller;

import com.beyond.MKX.domain.stock.dto.StockBriefDTO;
import com.beyond.MKX.domain.stock.dto.StockDetailResDTO;
import com.beyond.MKX.domain.stock.dto.StockListResDto;
import com.beyond.MKX.domain.stock.dto.StockInfoResDTO;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import com.beyond.MKX.domain.stock.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
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
    public StockDetailResDTO getStockDetail(@PathVariable String ticker) {
        return stockQueryService.getStockDetail(ticker);
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


    // 대량 간략 정보 조회 (이름 + id + status + delistingStage)
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
                        .build())
                .toList();
    }
}
