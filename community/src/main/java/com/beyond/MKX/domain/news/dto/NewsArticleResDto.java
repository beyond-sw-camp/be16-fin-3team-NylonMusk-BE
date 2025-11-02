package com.beyond.MKX.domain.news.dto;

import com.beyond.MKX.domain.news.entity.NewsArticle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticleResDto {
    private UUID id;
    private String title;
    private String link;
    private String publisher;
    private String author;
    private LocalDateTime publishedAt;
    private String thumbnailUrl;
    private String ticker;
    private UUID stockId;
    private String description;
    private String summary;

    public static NewsArticleResDto from(NewsArticle a) {
        return NewsArticleResDto.builder()
                .id(a.getId())
                .title(a.getTitle())
                .link(a.getLink())
                .publisher(a.getPublisher())
                .author(a.getAuthor())
                .publishedAt(a.getPublishedAt())
                .thumbnailUrl(a.getThumbnailUrl())
                .ticker(a.getTicker())
                .stockId(a.getStockId())
                .description(a.getDescription())
                .summary(a.getSummary())
                .build();
    }
}
