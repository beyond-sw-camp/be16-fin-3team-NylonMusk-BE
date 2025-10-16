package com.beyond.MKX.domain.ipo.allocation.dto;

import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    public static IpoAllocationItemResDTO from(IpoAllocation a) {
        return IpoAllocationItemResDTO.builder()
                .subscriptionId(a.getIpoSubscription().getId().toString())
                .allocatedPrice(a.getAllocatedPrice())
                .allocatedQuantity(a.getAllocatedQuantity())
                .allocatedAmount(a.getAllocatedAmount())
                .roundNo(a.getRoundNo())
                .allocatedAt(a.getAllocatedAt())
                .build();
    }
}


