package com.beyond.MKX.domain.stock.controller;

import com.beyond.MKX.domain.stock.dto.StockListResDto;
import com.beyond.MKX.domain.stock.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
}
