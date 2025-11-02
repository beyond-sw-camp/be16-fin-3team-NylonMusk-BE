package com.beyond.MKX.domain.news.controller;

import com.beyond.MKX.domain.news.dto.NewsArticleDetailResDto;
import com.beyond.MKX.domain.news.dto.NewsArticleResDto;
import com.beyond.MKX.domain.news.entity.NewsArticle;
import com.beyond.MKX.domain.news.repository.NewsArticleRepository;
import com.beyond.MKX.domain.news.repository.NewsArticleStockRepository;
import lombok.RequiredArgsConstructor;
import com.beyond.MKX.domain.news.service.NewsArticleQueryService;
import com.beyond.MKX.domain.news.service.NewsViewEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/news-articles")
@RequiredArgsConstructor
public class NewsArticleController {

    private final NewsArticleRepository repository;
    private final NewsArticleStockRepository stockRepository;
    private final NewsArticleQueryService newsArticleQueryService;
    private final NewsViewEventService viewEventService;

    @GetMapping
    public Page<NewsArticleResDto> list(
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return repository.search(emptyToNull(ticker), emptyToNull(q), pageable)
                .map(NewsArticleResDto::from);
    }

    @GetMapping("/{id}")
    public NewsArticleDetailResDto detail(@PathVariable("id") String id) {
        NewsArticle article = repository.findById(java.util.UUID.fromString(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var stocks = stockRepository.findByArticle_Id(article.getId());
        return NewsArticleDetailResDto.from(article, stocks);
    }

    @GetMapping("/by-stock")
    public Page<NewsArticleResDto> byStock(
            @RequestParam("stockId") String stockId,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID id = UUID.fromString(stockId);
        return repository.searchByStockId(id, emptyToNull(q), pageable)
                .map(NewsArticleResDto::from);
    }

    /** 여러 티커 배치 조회 */
    @GetMapping("/batch")
    public Page<NewsArticleResDto> batch(
            @RequestParam("tickers") java.util.List<String> tickers,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return newsArticleQueryService.getByTickers(tickers, q, pageable);
    }

    /** 인기순 조회 */
    @GetMapping("/popular")
    public Page<NewsArticleResDto> popular(
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return newsArticleQueryService.getPopular(emptyToNull(ticker), emptyToNull(q), pageable);
    }

    /** 조회수 증가 (뉴스 상세 조회 시 호출) */
    @PostMapping("/{id}/view")
    public void recordView(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestBody(required = false) Map<String, Integer> body
    ) {
        UUID newsId = UUID.fromString(id);
        UUID userId = getCurrentUserId(userIdHeader);
        Integer duration = body != null ? body.get("duration") : null;
        viewEventService.recordView(newsId, userId, duration);
    }

    /** 공유수 증가 */
    @PostMapping("/{id}/share")
    public void recordShare(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        UUID newsId = UUID.fromString(id);
        UUID userId = getCurrentUserId(userIdHeader);
        viewEventService.recordShare(newsId, userId);
    }

    private String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
    
    private UUID getCurrentUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null; // 비로그인 사용자
        }
        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            // 헤더 형식이 잘못된 경우 null 반환 (에러를 던지지 않고 조용히 처리)
            return null;
        }
    }
}
