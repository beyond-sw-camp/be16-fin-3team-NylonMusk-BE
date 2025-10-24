package com.beyond.MKX.domain.news.controller;

import com.beyond.MKX.domain.news.dto.NewsArticleResDto;
import com.beyond.MKX.domain.news.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/news-articles")
@RequiredArgsConstructor
public class NewsArticleController {

    private final NewsArticleRepository repository;

    @GetMapping
    public Page<NewsArticleResDto> list(
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return repository.search(emptyToNull(ticker), emptyToNull(q), pageable)
                .map(NewsArticleResDto::from);
    }

    private String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}

