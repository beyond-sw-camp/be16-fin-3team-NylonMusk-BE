package com.beyond.MKX.domain.news.controller;

import com.beyond.MKX.domain.news.service.NewsArticleRemappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 뉴스 관련 내부 API 컨트롤러
 * 마이크로서비스 간 통신용 엔드포인트
 */
@RestController
@RequestMapping("/api/internal/news-articles")
@RequiredArgsConstructor
public class NewsArticleInternalController {

    private final NewsArticleRemappingService remappingService;

    /** 상장된 종목의 기존 뉴스 재매핑 (내부 API) */
    @PostMapping("/remap")
    public Map<String, Object> remapNewsForListedStock(
            @RequestParam("stockId") String stockId,
            @RequestParam("stockNameKo") String stockNameKo,
            @RequestParam("ticker") String ticker
    ) {
        UUID id = UUID.fromString(stockId);
        int matchedCount = remappingService.remapNewsForListedStock(id, stockNameKo, ticker);
        return Map.of(
                "stockId", stockId,
                "ticker", ticker,
                "matchedCount", matchedCount
        );
    }
}
