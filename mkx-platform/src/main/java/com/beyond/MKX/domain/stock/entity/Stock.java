package com.beyond.MKX.domain.stock.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.delisting.entity.DelistingReason;
import com.beyond.MKX.domain.delisting.entity.DelistingStage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시/리플렉션용, 외부 new 방지
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더만 사용하도록
@ToString
@EqualsAndHashCode(of = "ticker") // 또는 of = "id" 선택
@Entity
@Table(
        name = "stock",
        indexes = {
                @Index(name = "uk_stock_ticker", columnList = "ticker", unique = true)
        }
)
public class Stock extends BaseIdAndTimeEntity {
    // ====== 외래 키(연관관계 미정 → UUID로 보유) ======
    @Column(name = "corporation_id", nullable = false)
    private UUID corporationId;

    // ====== 비즈니스 키 ======
    @Column(name = "ticker", length = 6, nullable = false)
    private String ticker;

    // ====== 한글 종목명 ======
    @Column(name = "name_ko", length = 50, nullable = false)
    private String nameKo;

    // ====== 상장/정지 상태 ======
    public enum Status { 
        LISTED,                    // 정상 상장
        SUSPENDED,                 // 거래 정지
        DELISTING_RISK,            // 상장폐지 위험 (기준 위반)
        DELISTING_NOTICE,          // 상장폐지 예고
        DELISTING_PROCESS,         // 상장폐지 절차 진행 중
        DELISTED                   // 상장폐지 완료
    }

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.LISTED; // DB 기본값과 일치

    // ====== 주식수 ======
    @Column(name = "total_shares_outstanding", nullable = false)
    private long totalSharesOutstanding; // NOT NULL

    @Column(name = "owned_shares")
    private Long ownedShares;            // NULL 허용

    @Column(name = "free_float_shares")
    private Long freeFloatShares;        // NULL 허용 (유통 가능 주식수)

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;     // 소프트 삭제 등 필요 시 사용

    // 상장폐지 관련 필드 추가
    @Column(name = "delisting_stage")
    @Enumerated(EnumType.STRING)
    private DelistingStage delistingStage = DelistingStage.NORMAL;

    @Column(name = "delisting_notice_date")
    private LocalDateTime delistingNoticeDate;

    @Column(name = "delisting_execution_date")
    private LocalDateTime delistingExecutionDate;

    @Column(name = "delisting_reason")
    @Enumerated(EnumType.STRING)
    private DelistingReason delistingReason;

    // ====== 종목 이미지 url ======
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    // ====== 도메인 메서드(선택적 변경만 허용) ======
    public void updateNameKo(String nameKo) {
        this.nameKo = nameKo;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    public void updateOwnedShares(Long ownedShares) {
        this.ownedShares = ownedShares;
    }

    public void updateFreeFloatShares(Long freeFloatShares) {
        this.freeFloatShares = freeFloatShares;
    }

    public void setDelistingNoticeDate(LocalDateTime delistingNoticeDate) {
        this.delistingNoticeDate = delistingNoticeDate;
    }

    public void setDelistingExecutionDate(LocalDateTime delistingExecutionDate) {
        this.delistingExecutionDate = delistingExecutionDate;
    }

    public void setDelistingStage(DelistingStage delistingStage) {
        this.delistingStage = delistingStage;
    }

    public void setDelistingReason(DelistingReason delistingReason) {
        this.delistingReason = delistingReason;
    }

    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
