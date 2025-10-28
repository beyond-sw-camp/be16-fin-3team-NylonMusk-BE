package com.beyond.MKX.domain.news.repository;

import com.beyond.MKX.domain.news.entity.NewsArticle;
import com.beyond.MKX.domain.news.entity.NewsArticleStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NewsArticleStockRepository extends JpaRepository<NewsArticleStock, UUID> {
    boolean existsByArticleAndStockId(NewsArticle article, UUID stockId);
    List<NewsArticleStock> findByArticle_Id(UUID articleId);
}
