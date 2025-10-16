package com.beyond.MKX.domain.ipo.allocation.dto;

import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IpoAllocationSummaryResDTO {
    private String offeringId;
    private Integer roundNo;
    private Long offerPrice;
    private Long offerQuantity;
    private Long allocatedTotalQuantity;
    private Integer allocationCount;
    private String offeringStatus;

    private List<IpoAllocationItemResDTO> allocations;

    public static IpoAllocationSummaryResDTO of(IpoOffering o,
                                                List<IpoAllocation> list,
                                                long allocatedTotalQuantity) {
        return IpoAllocationSummaryResDTO.builder()
                .offeringId(o.getId().toString())
                .roundNo(o.getRoundNo())
                .offerPrice(o.getOfferPrice())
                .offerQuantity(o.getOfferQuantity())
                .allocatedTotalQuantity(allocatedTotalQuantity)
                .allocationCount(list == null ? 0 : list.size())
                .offeringStatus(o.getIpoOfferingStatus().name())
                .allocations(list == null ? List.of()
                        : list.stream().map(IpoAllocationItemResDTO::from).toList())
                .build();
    }
}
