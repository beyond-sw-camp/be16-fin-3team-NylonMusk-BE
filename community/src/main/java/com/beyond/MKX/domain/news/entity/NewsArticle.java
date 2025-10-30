package com.beyond.MKX.domain.news.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "news_article", indexes = {
        @Index(name = "uk_news_article_link", columnList = "link", unique = true)
})
public class NewsArticle extends BaseIdAndTimeEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "link", nullable = false, length = 512)
    private String link;

    @Column(name = "publisher", length = 80)
    private String publisher;

    @Column(name = "author", length = 100)
    private String author;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Column(name = "stock_id")
    private UUID stockId;

    @Column(name = "ticker", length = 16)
    private String ticker;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    public void setSummary(String summary) { this.summary = summary; }
    public void setAuthor(String author) { this.author = author; }
}
