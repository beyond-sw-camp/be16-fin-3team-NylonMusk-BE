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
@Table(name = "news_view_event", indexes = {
        @Index(name = "idx_news_view_event_news_id", columnList = "news_id"),
        @Index(name = "idx_news_view_event_created_at", columnList = "created_at"),
        @Index(name = "idx_news_view_event_news_created", columnList = "news_id, created_at")
})
public class NewsViewEvent extends BaseIdAndTimeEntity {

    @Column(name = "news_id", nullable = false)
    private UUID newsId;

    @Column(name = "user_id")
    private UUID userId; // nullable (비로그인 사용자도 가능)

    @Column(name = "event_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(name = "duration_seconds")
    private Integer durationSeconds; // 체류시간 (초 단위, VIEW 이벤트에만 사용)

    public enum EventType {
        VIEW,   // 조회
        SHARE   // 공유
    }
}

