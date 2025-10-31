package com.beyond.MKX.domain.news.service;

import com.beyond.MKX.domain.news.dto.NewsArticleResDto;
import com.beyond.MKX.domain.news.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsArticleQueryService {

    private final NewsArticleRepository newsArticleRepository;

    public Page<NewsArticleResDto> getByTickers(List<String> tickers, String q, Pageable pageable) {
        return newsArticleRepository
                .searchByTickers(tickers, (q == null || q.isBlank()) ? null : q, pageable)
                .map(NewsArticleResDto::from);
    }

    public Page<NewsArticleResDto> getPopular(String ticker, String q, Pageable pageable) {
        return newsArticleRepository
                .findPopular(
                        (ticker == null || ticker.isBlank()) ? null : ticker,
                        (q == null || q.isBlank()) ? null : q,
                        pageable
                )
                .map(NewsArticleResDto::from);
    }
}


