package com.beyond.MKX.domain.news.dto;

import com.beyond.MKX.domain.news.entity.News;
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
public class NewsResDto {
    private UUID id;
    private String title;
    private String sourceUrl;
    private String sourceName;
    private LocalDateTime publishedAt;

    public static NewsResDto from(News news) {
        return NewsResDto.builder()
                .id(news.getId())
                .title(news.getTitle())
                .sourceUrl(news.getSourceUrl())
                .sourceName(news.getSourceName())
                .publishedAt(news.getPublishedAt())
                .build();
    }
}
