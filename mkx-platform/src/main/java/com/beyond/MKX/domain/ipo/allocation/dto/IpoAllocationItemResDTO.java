package com.beyond.MKX.domain.ipo.allocation.dto;

import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IpoAllocationItemResDTO {
    private String subscriptionId;
    private Long allocatedPrice;
    private Long allocatedQuantity;
    private Long allocatedAmount;
    private Integer roundNo;
    private LocalDateTime allocatedAt;

    // ▼ 추가: 계산 필드
    private Long depositAmount;     // subscription.requiredDeposit (스냅샷)
    private Long finalAmount;       // allocatedQuantity * allocatedPrice
    private Long additionalAmount;  // max(final - deposit, 0)
    private Long refundAmount;      // max(deposit - final, 0)

    public static IpoAllocationItemResDTO from(IpoAllocation a) {
        // 기본값
        Long price = a.getAllocatedPrice();
        Long qty   = a.getAllocatedQuantity();
        long finalAmt   = (price == null || qty == null) ? 0L : Math.multiplyExact(price, qty);

        // requiredDeposit: Allocation -> Subscription 경유
        Long deposit = Optional.ofNullable(a.getIpoSubscription())
                .map(s -> s.getRequiredDeposit())
                .orElse(0L);

        long additional = Math.max(finalAmt - deposit, 0L);
        long refund     = Math.max(deposit - finalAmt, 0L);

        return IpoAllocationItemResDTO.builder()
                .subscriptionId(a.getIpoSubscription().getId().toString())
                .allocatedPrice(a.getAllocatedPrice())
                .allocatedQuantity(a.getAllocatedQuantity())
                .allocatedAmount(a.getAllocatedAmount())
                .roundNo(a.getRoundNo())
                .allocatedAt(a.getAllocatedAt())
                .depositAmount(deposit)
                .finalAmount(finalAmt)
                .additionalAmount(additional)
                .refundAmount(refund)
                .build();
    }
}


