package com.beyond.MKX.domain.stock.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
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
    @Column(name = "ticker", length = 20, nullable = false)
    private String ticker;

    // ====== 한글 종목명 ======
    @Column(name = "name_ko", length = 50, nullable = false)
    private String nameKo;

    // ====== 상장/정지 상태 ======
    public enum Status { LISTED, SUSPENDED }

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

    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
