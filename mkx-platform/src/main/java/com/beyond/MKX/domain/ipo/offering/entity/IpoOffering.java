package com.beyond.MKX.domain.ipo.offering.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "ipo_offering",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_offering_ipo_round",
                        columnNames = {"ipo_id", "round_no"}
                )
        }
)
public class IpoOffering extends BaseIdAndTimeEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ipo_id")
    private Ipo ipo;
    /* 확정 공모가 */
    private Long offerPrice;
    /* 예정 공모 물량 */
    private Long offerQuantity;
    /* 공모 배정 물량 */
    private Long allocatedQuantity;
    /* 공모 확정 물량 */
    private Long issuedQuantity;
    /* 청약 단위 */
    private Integer lotSize;

    /* 청약 시작 시간 */
    private LocalDateTime subscriptionStart;
    /* 청약 마감 시간 */
    private LocalDateTime subscriptionEnd;
    /* 배정일 */
    private LocalDate allocationDate;
    /* 환불 기준일 */
    private LocalDate refundDate;

    /* 희망 공모가 최솟값 */
    private Long priceBandMin;
    /* 희망 공모가 최댓값 */
    private Long priceBandMax;
    /* 증거금률 */
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal depositRate;
    /* 상태 */
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private IpoOfferingStatus ipoOfferingStatus;

    /* 청약 경쟁률 */
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal competitionRatio;

    /* 공모 차수 */
    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    /* 확정공모가 확정 시각 */
    @Column(name = "price_fixed_at")
    private LocalDateTime priceFixedAt;

    // 수요예측 시작 시간
    @Column(nullable = true)
    private LocalDateTime bookBuildingStart;

    // 수요예측 마감 시간
    @Column(nullable = true)
    private LocalDateTime bookBuildingEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IpoOfferingType offeringType = IpoOfferingType.INITIAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_offering_id")
    private IpoOffering baseOffering; // N차 공모의 모체

    @Column(name = "record_date")
    private LocalDateTime recordDate; // 기준일 (거래정지·권리락용)

    public void offeringOpen(java.time.LocalDateTime now) {
        if (this.ipoOfferingStatus != IpoOfferingStatus.PRICE_FIXED) {
            throw new IllegalArgumentException("PRICE_FIXED 상태에서만 OPEN 가능");
        }
        this.ipoOfferingStatus = IpoOfferingStatus.OPEN;
    }

    public void offeringCloseNow(LocalDateTime now) {
        if (this.ipoOfferingStatus != IpoOfferingStatus.OPEN) {
            throw new IllegalStateException("OPEN에서만 CLOSED 가능");
        }
        this.ipoOfferingStatus = IpoOfferingStatus.CLOSED;
    }

    public void setCompetitionRatio(BigDecimal ratio) {
        if (ratio == null) throw new IllegalArgumentException("competitionRatio는 null 불가");
        // 컬럼이 precision=5, scale=2 이므로 소수 둘째자리까지만 유지
        this.competitionRatio = ratio.setScale(2, RoundingMode.HALF_UP);
    }

    public void setIpoOfferingStatus(IpoOfferingStatus status) {
        this.ipoOfferingStatus = status;
    }

    public void fixOfferPrice(long price, long min, long max, long face) {
        if (this.ipoOfferingStatus != IpoOfferingStatus.BOOK_BUILDING) {
            throw new IllegalStateException("BOOK_BUILDING 에서만 가격 확정 가능");
        }
        if (price < face) throw new IllegalArgumentException("확정 공모가는 액면가(" + face + ") 이상이어야 합니다.");
        if (price < min || price > max) throw new IllegalArgumentException("확정 공모가는 밴드 범위(" + min + "~" + max + ") 내여야 합니다.");
        this.offerPrice = price;
        this.ipoOfferingStatus = IpoOfferingStatus.PRICE_FIXED;
        this.priceFixedAt = LocalDateTime.now();
    }

    public void offeringCancel() {
        if (this.ipoOfferingStatus == IpoOfferingStatus.CANCELLED
                || this.ipoOfferingStatus == IpoOfferingStatus.SETTLED) {
            throw new IllegalStateException("이미 종결/취소됨");
        }
        this.ipoOfferingStatus = IpoOfferingStatus.CANCELLED;
    }

    public void allocated(long totalAllocated) {
        if (this.ipoOfferingStatus != IpoOfferingStatus.CLOSED && this.ipoOfferingStatus != IpoOfferingStatus.VERIFIED) {
            throw new IllegalStateException("CLOSED 또는 VERIFIED 이후에만 배정 확정 가능");
        }
        if (totalAllocated < 0 || totalAllocated > this.offerQuantity) {
            throw new IllegalStateException("배정 수량이 유효하지 않습니다.");
        }
        this.allocatedQuantity = totalAllocated;
        this.ipoOfferingStatus = IpoOfferingStatus.ALLOCATED;
    }

    public void settle(long issuedQuantity) {
        if (this.ipoOfferingStatus != IpoOfferingStatus.ALLOCATED) {
            throw new IllegalStateException("ALLOCATED 이후에만 정산 가능");
        }
        if (issuedQuantity < 0 || issuedQuantity > this.allocatedQuantity) {
            throw new IllegalStateException("정산 수량이 유효하지 않습니다.");
        }
        this.issuedQuantity = issuedQuantity;
        this.ipoOfferingStatus = IpoOfferingStatus.SETTLED;
    }

    public void approveOffering() {
        if (this.ipoOfferingStatus != IpoOfferingStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 상태에서만 승인 가능합니다.");
        }
        this.ipoOfferingStatus = IpoOfferingStatus.SCHEDULED;
    }

}
