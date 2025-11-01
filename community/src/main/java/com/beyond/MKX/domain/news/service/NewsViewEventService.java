package com.beyond.MKX.domain.news.service;

import com.beyond.MKX.domain.news.entity.NewsViewEvent;
import com.beyond.MKX.domain.news.repository.NewsViewEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NewsViewEventService {

    private final NewsViewEventRepository viewEventRepository;

    @Transactional
    public void recordView(UUID newsId, UUID userId, Integer durationSeconds) {
        NewsViewEvent event = NewsViewEvent.builder()
                .newsId(newsId)
                .userId(userId)
                .eventType(NewsViewEvent.EventType.VIEW)
                .durationSeconds(durationSeconds)
                .build();
        viewEventRepository.save(event);
    }

    @Transactional
    public void recordShare(UUID newsId, UUID userId) {
        NewsViewEvent event = NewsViewEvent.builder()
                .newsId(newsId)
                .userId(userId)
                .eventType(NewsViewEvent.EventType.SHARE)
                .build();
        viewEventRepository.save(event);
    }
}

