package com.beyond.MKX.domain.news.repository;

import com.beyond.MKX.domain.news.entity.NewsViewEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NewsViewEventRepository extends JpaRepository<NewsViewEvent, UUID> {
}

