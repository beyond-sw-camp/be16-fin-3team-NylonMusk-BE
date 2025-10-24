package com.beyond.MKX.domain.news.service;


import com.beyond.MKX.domain.news.dto.NewsReqDto;
import com.beyond.MKX.domain.news.entity.News;
import com.beyond.MKX.domain.news.dto.NewsResDto;
import com.beyond.MKX.domain.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final MkStockNewsCrawler mkStockNewsCrawler;

    @Transactional
    public List<NewsResDto> refreshNews(String keyword) {
        List<NewsReqDto> latestNews = mkStockNewsCrawler.fetchStockNews(keyword);
        List<NewsResDto> saved = new ArrayList<>();

        for (NewsReqDto dto : latestNews) {
            // URL 기준 중복 방지
            if (!newsRepository.existsBySourceUrl(dto.getSourceUrl())) {
                News news = News.builder()
                        .title(dto.getTitle())
                        .sourceUrl(dto.getSourceUrl())
                        .sourceName(dto.getSourceName())
                        .publishedAt(dto.getPublishedAt())
                        .build();
                saved.add(NewsResDto.from(newsRepository.save(news)));
            }
        }

        return saved;
    }

    public Page<NewsResDto> getAllNews(Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "publishedAt"));

        Page<News> page = newsRepository.findAll(sorted);
        return new PageImpl<>(
                page.getContent().stream().map(NewsResDto::from).collect(Collectors.toList()),
                page.getPageable(),
                page.getTotalElements()
        );
    }
}
