package com.beyond.MKX.domain.ipo.subscription.dto;

import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.subscription.entity.IpoSubscription;
import com.beyond.MKX.domain.ipo.subscription.entity.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpoSubscriptionResDTO {
    private UUID id;
    private UUID ipoOfferingId;
    private UUID accountId;
    private Long appliedQuantity;
    private Long offerPriceSnapshot;
    private BigDecimal depositRateSnapshot;
    private Long requiredDeposit;
    private Long heldDeposit;       // 파생: 현재 보유(= required - refunded)
    private Long refundedAmount;
    private SubscriptionStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private BigDecimal competitionRatioX;       // 예: 1.23  (표시는 FE에서 "1.23:1")


    public static IpoSubscriptionResDTO from(IpoSubscription subscription) {
        long requiredDeposit = subscription.getRequiredDeposit() == null ? 0L : subscription.getRequiredDeposit();
        long refundedAmount = subscription.getRefundedAmount() == null ? 0L : subscription.getRefundedAmount();
        long held = Math.max(requiredDeposit - refundedAmount, 0L);

        return IpoSubscriptionResDTO.builder()
                .id(subscription.getId())
                .ipoOfferingId(subscription.getIpoOffering().getId())
                .accountId(subscription.getAccountId())
                .appliedQuantity(subscription.getAppliedQuantity())
                .offerPriceSnapshot(subscription.getOfferPriceSnapshot())
                .depositRateSnapshot(subscription.getDepositRateSnapshot())
                .requiredDeposit(requiredDeposit)
                .heldDeposit(held)                 // ← 여기만 파생
                .refundedAmount(refundedAmount)
                .status(subscription.getStatus())
                .appliedAt(subscription.getAppliedAt())
                .paidAt(subscription.getPaidAt())
                .cancelledAt(subscription.getCancelledAt())
                .competitionRatioX(null)
                .build();
    }
}
