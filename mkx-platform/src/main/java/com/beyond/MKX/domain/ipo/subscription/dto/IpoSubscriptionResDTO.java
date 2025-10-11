package com.beyond.MKX.domain.ipo.subscription.dto;

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
    private Long depositAmount;
    private Long refundedAmount;
    private SubscriptionStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    public static IpoSubscriptionResDTO from(IpoSubscription subscription) {
        return new IpoSubscriptionResDTO(
                subscription.getId(),
                subscription.getIpoOffering().getId(),
                subscription.getAccountId(),
                subscription.getAppliedQuantity(),
                subscription.getOfferPriceSnapshot(),
                subscription.getDepositRateSnapshot(),
                subscription.getRequiredDeposit(),
                subscription.getDepositAmount(),
                subscription.getRefundedAmount(),
                subscription.getStatus(),
                subscription.getAppliedAt(),
                subscription.getPaidAt(),
                subscription.getCancelledAt()
            );
    }
}
