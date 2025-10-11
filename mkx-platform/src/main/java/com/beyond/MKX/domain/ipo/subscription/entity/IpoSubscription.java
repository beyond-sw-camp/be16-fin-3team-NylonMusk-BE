package com.beyond.MKX.domain.ipo.subscription.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        // 계좌 1개당 청약 1번
        uniqueConstraints = {
                @UniqueConstraint(name="uk_sub_offering_account",
                        columnNames={"ipo_offering_id","account_id"})
        }
)
public class IpoSubscription extends BaseIdAndTimeEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ipo_offering_id")
    private IpoOffering ipoOffering;

    @Enumerated(EnumType.STRING)
    private InvestorType investorType;

    @Column(name = "subscriber_id", nullable = false)
    private UUID subscriberId;
    @Column(name = "brokerage_id", nullable = false)
    private UUID brokerageId;
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    // 신청 정보
    @Column(nullable=false)
    private Long appliedQuantity;  // 신청 수량(주)
    @Column(nullable=false)
    private Long offerPriceSnapshot;              // 청약 시점 단가(원)
    @Column(precision=5, scale=2, nullable=false)
    private BigDecimal depositRateSnapshot; // 증거금률 스냅샷(%)

    // 금액(원, 정수)
    @Column(nullable=false)
    private Long requiredDeposit;  // 필요 증거금
    @Column(nullable=false)
    @Builder.Default private
    Long paidAmount = 0L;      // 납입 금액
    @Column(nullable=false)
    @Builder.Default
    private Long refundedAmount = 0L;  // 환불 금액

    // 상태/시각
    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.APPLIED;

    @Column(nullable=false)
    private LocalDateTime appliedAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;

    // 청약 총액 계산 메서드
    public Long appliedAmount() { return appliedQuantity * offerPriceSnapshot; }
    public void setPaidAmount(Long paidAmount) { this.paidAmount = paidAmount; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
