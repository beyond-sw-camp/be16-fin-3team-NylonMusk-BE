package com.beyond.MKX.domain.news.repository;

import com.beyond.MKX.domain.news.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NewsRepository extends JpaRepository<News, UUID> {
    boolean existsBySourceUrl(String sourceUrl);
}
