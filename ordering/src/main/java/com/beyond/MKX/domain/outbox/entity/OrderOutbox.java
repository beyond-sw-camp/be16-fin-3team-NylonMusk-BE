package com.beyond.MKX.domain.outbox.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "order_outbox", indexes = {
        @Index(name = "idx_order_outbox_is_published", columnList = "is_published, created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderOutbox {
    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_log_id", nullable = false)
    private UUID orderLogId;

    // ORDER_PLACED (주문 접수)
    @Column(name = "event_type", nullable = false, columnDefinition = "VARCHAR(50)")
    private String eventType;

    @Column(nullable = false)
    private String kafkaKey;

    /**
     * @JdbcTypeCode(SqlTypes.JSON)이란?
     * Hibernate에게 이 필드가 SQL의 JSON 계열 타입과 매핑이 된다고 알려주는 역할을 함.
     */
    @JdbcTypeCode(SqlTypes.JSON) // Hibernate 6+ 방식, JSON 타입 매핑
    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private boolean isPublished = false; // 기본값 false 설정

    @CreationTimestamp // 엔티티 생성 시 자동으로 현재 시간 저장
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void markAsPublished() {
        this.isPublished = true;
    }
    public void revertToUnpublished() {
        this.isPublished = false;
    }

}
