package com.beyond.MKX.domain.news.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "news_article_stock",
        uniqueConstraints = @UniqueConstraint(name = "uk_article_stock", columnNames = {"article_id", "stock_id"})
)
public class NewsArticleStock extends BaseIdAndTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private NewsArticle article;

    @Column(name = "stock_id", nullable = false)
    private UUID stockId;

    @Column(name = "ticker", length = 16)
    private String ticker;

    @Column(name = "stock_name", length = 100)
    private String stockName;
}

