package com.beyond.MKX.domain.news.controller;

import com.beyond.MKX.domain.news.dto.NewsArticleDetailResDto;
import com.beyond.MKX.domain.news.dto.NewsArticleResDto;
import com.beyond.MKX.domain.news.entity.NewsArticle;
import com.beyond.MKX.domain.news.repository.NewsArticleRepository;
import com.beyond.MKX.domain.news.repository.NewsArticleStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/news-articles")
@RequiredArgsConstructor
public class NewsArticleController {

    private final NewsArticleRepository repository;
    private final NewsArticleStockRepository stockRepository;

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

    private String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}
