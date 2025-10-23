package com.beyond.MKX.domain.ipo.allocation.dto;

import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import com.beyond.MKX.domain.ipo.subscription.entity.IpoSubscription;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IpoAllocationItemResDTO {
    // 기본 배정 정보
    private String subscriptionId;
    private Long allocatedPrice;
    private Long allocatedQuantity;
    private Long allocatedAmount;
    private Integer roundNo;
    private LocalDateTime allocatedAt;
    private String status;  // 배정 상태 (COMPLETED, PENDING)

    // ▼ 추가: 구독자(청약자) 식별/요약 정보
    private String subscriberId;   // IpoSubscription.subscriberId
    private String accountId;      // IpoSubscription.accountId
    private String brokerageId;    // IpoSubscription.brokerageId
    private String investorType;   // IpoSubscription.investorType.name()

    // ▼ 추가: 계산 필드
    private Long depositAmount;     // subscription.requiredDeposit (스냅샷)
    private Long finalAmount;       // allocatedQuantity * allocatedPrice
    private Long additionalAmount;  // max(final - deposit, 0)
    private Long refundAmount;      // max(deposit - final, 0)

    public static IpoAllocationItemResDTO from(IpoAllocation a) {
        IpoSubscription s = a.getIpoSubscription();

        Long price = a.getAllocatedPrice();
        Long qty   = a.getAllocatedQuantity();
        long finalAmt = (price == null || qty == null) ? 0L : Math.multiplyExact(price, qty);

        Long deposit = Optional.ofNullable(s)
                .map(IpoSubscription::getRequiredDeposit)
                .orElse(0L);

        long additional = Math.max(finalAmt - deposit, 0L);
        long refund     = Math.max(deposit - finalAmt, 0L);

        return IpoAllocationItemResDTO.builder()
                // 배정 기본
                .subscriptionId(s.getId().toString())
                .allocatedPrice(price)
                .allocatedQuantity(qty)
                .allocatedAmount(a.getAllocatedAmount())
                .roundNo(a.getRoundNo())
                .allocatedAt(a.getAllocatedAt())
                .status(a.getStatus() != null ? a.getStatus().name() : null)
                // 구독자 식별/요약
                .subscriberId(nvlUUID(s.getSubscriberId()))
                .accountId(nvlUUID(s.getAccountId()))
                .brokerageId(nvlUUID(s.getBrokerageId()))
                .investorType(s.getInvestorType() != null ? s.getInvestorType().name() : null)
                // 계산 필드
                .depositAmount(deposit)
                .finalAmount(finalAmt)
                .additionalAmount(additional)
                .refundAmount(refund)
                .build();
    }

    private static String nvlUUID(UUID id) {
        return id == null ? null : id.toString();
    }
}
