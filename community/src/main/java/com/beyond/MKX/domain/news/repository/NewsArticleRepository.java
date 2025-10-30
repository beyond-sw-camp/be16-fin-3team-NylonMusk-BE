package com.beyond.MKX.domain.news.repository;

import com.beyond.MKX.domain.news.entity.NewsArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {
    boolean existsByLink(String link);

    @Query("""
            select n from NewsArticle n
            where (:ticker is null or n.ticker = :ticker)
              and (
                    :q is null or lower(n.title) like lower(concat('%', :q, '%'))
                 or lower(n.description) like lower(concat('%', :q, '%'))
              )
            order by n.publishedAt desc
            """)
    Page<NewsArticle> search(
            @Param("ticker") String ticker,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
            select a from NewsArticleStock s
            join s.article a
            where s.stockId = :stockId
              and (
                    :q is null or lower(a.title) like lower(concat('%', :q, '%'))
                 or lower(a.description) like lower(concat('%', :q, '%'))
              )
            order by a.publishedAt desc
            """)
    Page<NewsArticle> searchByStockId(
            @Param("stockId") UUID stockId,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
            select n from NewsArticle n
            where (:tickers is null or n.ticker in :tickers)
              and (
                    :q is null or lower(n.title) like lower(concat('%', :q, '%'))
                 or lower(n.description) like lower(concat('%', :q, '%'))
              )
            order by n.publishedAt desc
            """)
    Page<NewsArticle> searchByTickers(
            @Param("tickers") java.util.List<String> tickers,
            @Param("q") String q,
            Pageable pageable
    );
}
