package com.beyond.MKX.domain.ipo.offering.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    /* 공모 물량 */
    private Long offerQuantity;
    /* 청약 단위 */
    private Long lotSize;

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
    private IpoOfferingStatus ipoOfferingStatus;

    /* 잔여 주식 배분 */
    // TODO: 계좌 생성 이후 진행 할 예정

    /* 상한 비율 */
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal capRatio;
    /* 청약 경쟁률 */
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal competitionRatio;

    /* 공모 차수 */
    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    /* 배정 방식 */
    // TODO: 계좌 생성 이후 진행 할 예정

    public void offeringOpen(java.time.LocalDateTime now) {
        if (this.ipoOfferingStatus != IpoOfferingStatus.SCHEDULED) {
            throw new IllegalArgumentException("SCHEDULED 상태에서만 OPEN 가능");
        }
        this.ipoOfferingStatus = IpoOfferingStatus.OPEN;
    }

    public void offeringCloseNow(java.time.LocalDateTime now) {
        if (this.ipoOfferingStatus != IpoOfferingStatus.OPEN) {
            throw new IllegalStateException("OPEN에서만 CLOSED 가능");
        }
        this.ipoOfferingStatus = IpoOfferingStatus.CLOSED;
    }

    public void fixOfferPrice(long price, long min, long max, long face) {
        if (this.ipoOfferingStatus != IpoOfferingStatus.CLOSED) {
            throw new IllegalStateException("CLOSED에서만 가격 확정 가능");
        }
        if (price < face) throw new IllegalArgumentException("확정 공모가는 액면가 이상");
        if (price < min || price > max) throw new IllegalArgumentException("밴드 범위 이탈");
        this.offerPrice = price;
        this.ipoOfferingStatus = IpoOfferingStatus.PRICE_FIXED;
    }

    public void offeringCancel() {
        if (this.ipoOfferingStatus == IpoOfferingStatus.CANCELLED
                || this.ipoOfferingStatus == IpoOfferingStatus.SETTLED) {
            throw new IllegalStateException("이미 종결/취소됨");
        }
        this.ipoOfferingStatus = IpoOfferingStatus.CANCELLED;
    }
}
