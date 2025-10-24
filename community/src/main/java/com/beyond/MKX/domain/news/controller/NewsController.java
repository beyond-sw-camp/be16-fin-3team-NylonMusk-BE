package com.beyond.MKX.domain.news.controller;


import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.news.dto.NewsResDto;
import com.beyond.MKX.domain.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/public/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    // 뉴스 갱신 (관리자 수동)
    @PostMapping("/refresh")
    public List<NewsResDto> refreshNews(@RequestParam String keyword) {
        return newsService.refreshNews(keyword);
    }

    @GetMapping
    public Page<NewsResDto> getAllNews(Pageable pageable) {
        return newsService.getAllNews(pageable);
    }
}
