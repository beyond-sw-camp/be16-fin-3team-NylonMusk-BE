package com.beyond.MKX.domain.ipo.allocation.outbound;

import lombok.Builder;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "ordering-service")
public interface OrderingHoldingClient {

    @PostMapping("/internal/holdings/allocate/batch")
    void allocateBatch(@RequestBody AllocateHoldingsBatchReq req);

    @Builder
    record AllocateHoldingsBatchReq(List<Item> items) {
        @Builder
        public record Item(
                UUID allocationEventId,
                UUID memberAccountId,
                UUID brokerageId,
                String ticker,
                long quantity,
                long pricePerShare,
                long lockedQuantity
        ){}
    }
}
