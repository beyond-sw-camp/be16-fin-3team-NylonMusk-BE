package com.beyond.MKX.domain.ipo.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "ipo_allocation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_allocation_subscription_round",
                        columnNames = {"ipo_subscription_id", "round_no"}
                )
        }
)
public class IpoAllocation extends BaseIdAndTimeEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ipo_subscription_id", nullable = false)
    private IpoSubscription ipoSubscription;

    // 배정 결과 (원 단위 정수)
    /* 배정 단가 */
    @Column(nullable=false)
    private Long allocatedPrice;     // 보통 offering.offerPrice
    /* 배정 수량(주) */
    @Column(nullable=false)
    private Long allocatedQuantity;
    /* 배정 금액 */
    @Column(nullable=false)
    private Long allocatedAmount;    // allocatedPrice * allocatedQuantity

    /* 배정 일자 */
    private LocalDateTime allocatedAt;

    /* 배정 차수 */
    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    public static IpoAllocation of(IpoSubscription ipoSubscription, long allocatedQuantity, long allocatedPrice, int roundNo, LocalDateTime now) {
        if (ipoSubscription == null) throw new IllegalArgumentException("배정을 위해선 청약이 필수입니다.");
        if (allocatedQuantity <= 0 || allocatedPrice <= 0) throw new IllegalArgumentException("청약 가격/수량은 양수여야 합니다.");
        if (ipoSubscription.getAppliedQuantity() != null && allocatedQuantity > ipoSubscription.getAppliedQuantity()) {
            throw new IllegalArgumentException("배정 수량이 청약 수량을 초과하였습니다.");
        }
        long amount = Math.multiplyExact(allocatedPrice, allocatedQuantity); // 안전 곱셈
        return IpoAllocation.builder()
                .ipoSubscription(ipoSubscription)
                .allocatedPrice(allocatedPrice)
                .allocatedQuantity(allocatedQuantity)
                .allocatedAmount(amount)
                .roundNo(roundNo)
                .allocatedAt(now != null ? now : LocalDateTime.now())
                .build();
    }
}
