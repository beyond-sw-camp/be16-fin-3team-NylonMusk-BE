package com.beyond.MKX.domain.outbox.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "order_outbox"
//        ,indexes = {
//        @Index(name = "idx_is_published_created_at", columnList = "is_published, created_at")}
)
@Builder
public class OrderOutbox {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "order_log_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID orderLogId;

    // ORDER_PLACED (주문 접수)
    @Column(name = "event_type", nullable = false, columnDefinition = "VARCHAR(50)")
    private String eventType;

    @Column(nullable = false)
    private String kafkaKey;

    // JSON -> TEXT로 변경 (Debezium 파서 호환)
    // 문자열 그대로 저장. 필요 시 앱 레이어에서 직렬화/역직렬화
    @Lob // TEXT 매핑. (또는 @Column(columnDefinition = "TEXT"))
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    // TIMESTAMP(6) 생성 시각 (UTC 저장 권장)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    private LocalDateTime createdAt;

    /*
     * Polling 방식에서 CDC(Outbox + Debezium)로 전환하면서 outbox는 INSERT-only로 운영합니다.
     * is_published 플래그와 관련 메서드는 더 이상 사용하지 않아 혼선을 막기 위해 비활성화했습니다.
     * 이 필드는 스키마 마이그레이션 시 제거될 예정입니다.
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private boolean isPublished = false;

    public void markAsPublished() {
        this.isPublished = true;
    }
    public void revertToUnpublished() {
        this.isPublished = false;
    }
     */
}
