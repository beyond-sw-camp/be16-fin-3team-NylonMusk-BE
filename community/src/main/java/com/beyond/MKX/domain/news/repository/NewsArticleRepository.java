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

    /**
     * 인기순 정렬: 최근 24시간 내 조회수, 평균 체류시간, 공유수 기반 점수 계산
     * 점수 = (조회수 × 1.0) + (평균 체류시간(초) × 0.1) + (공유수 × 2.0)
     */
    @Query(value = """
            select n.* from news_article n
            left join (
                select 
                    news_id,
                    count(case when event_type = 'VIEW' then 1 end) as view_count,
                    coalesce(avg(case when event_type = 'VIEW' and duration_seconds > 0 then duration_seconds end), 0) as avg_duration,
                    count(case when event_type = 'SHARE' then 1 end) as share_count
                from news_view_event
                where created_at >= date_sub(now(), interval 24 hour)
                group by news_id
            ) v on n.id = v.news_id
            where (:ticker is null or n.ticker = :ticker)
              and (:q is null or lower(n.title) like lower(concat('%', :q, '%'))
                 or lower(n.description) like lower(concat('%', :q, '%')))
            order by 
                (coalesce(v.view_count, 0) * 1.0 + 
                 coalesce(v.avg_duration, 0) * 0.1 + 
                 coalesce(v.share_count, 0) * 2.0) desc,
                n.published_at desc
            """, nativeQuery = true)
    Page<NewsArticle> findPopular(
            @Param("ticker") String ticker,
            @Param("q") String q,
            Pageable pageable
    );

    /**
     * 서머리가 없는 뉴스 조회 (최신순)
     */
    @Query("""
            select n from NewsArticle n
            where n.summary is null or n.summary = ''
            order by n.publishedAt desc
            """)
    Page<NewsArticle> findBySummaryIsNull(Pageable pageable);
}
