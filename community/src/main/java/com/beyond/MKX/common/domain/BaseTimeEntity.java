package com.beyond.MKX.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@MappedSuperclass
@Getter
public abstract class BaseTimeEntity {
//    RDB에 UTC로 저장
    private static final ZoneId ZONE_ID = ZoneId.of("UTC");

    @Column(name = "created_at", updatable = false, nullable = false)
    protected LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    protected LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    protected LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** 소프트 삭제 처리 */
    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    /** 소프트 삭제 복구 */
    public void restore() {
        this.deletedAt = null;
    }

    /** 삭제 여부 확인 */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}