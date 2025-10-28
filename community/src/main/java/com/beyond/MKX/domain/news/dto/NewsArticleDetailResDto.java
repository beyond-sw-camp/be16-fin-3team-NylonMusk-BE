package com.beyond.MKX.domain.news.dto;

import com.beyond.MKX.domain.news.entity.NewsArticle;
import com.beyond.MKX.domain.news.entity.NewsArticleStock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticleDetailResDto {
    private UUID id;
    private String title;
    private String link;
    private String publisher;
    private String author;
    private LocalDateTime publishedAt;
    private String thumbnailUrl;
    private String description;
    private String summary;
    private String sentiment;
    private String ticker;     // 첫 매칭 종목(호환용)
    private UUID stockId;      // 첫 매칭 종목 ID(호환용)
    private List<StockRef> stocks; // 매칭된 전체 종목

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockRef {
        private UUID stockId;
        private String ticker;
        private String stockName;
    }

    public static NewsArticleDetailResDto from(NewsArticle a, List<NewsArticleStock> list) {
        return NewsArticleDetailResDto.builder()
                .id(a.getId())
                .title(a.getTitle())
                .link(a.getLink())
                .publisher(a.getPublisher())
                .author(a.getAuthor())
                .publishedAt(a.getPublishedAt())
                .thumbnailUrl(a.getThumbnailUrl())
                .description(a.getDescription())
                .summary(a.getSummary())
                .sentiment(a.getSentiment())
                .ticker(a.getTicker())
                .stockId(a.getStockId())
                .stocks(list.stream().map(s -> StockRef.builder()
                        .stockId(s.getStockId())
                        .ticker(s.getTicker())
                        .stockName(s.getStockName())
                        .build()).toList())
                .build();
    }
}

