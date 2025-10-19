package com.beyond.MKX.domain.assets.service;

import com.beyond.MKX.domain.assets.controller.AllocateHoldingsBatchReq;
import com.beyond.MKX.domain.assets.entity.AllocationAppliedLog;
import com.beyond.MKX.domain.assets.entity.StockHolding;
import com.beyond.MKX.domain.assets.repository.AllocationAppliedLogRepository;
import com.beyond.MKX.domain.assets.repository.StockHoldingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HoldingAllocateService {

    private final StockHoldingRepository holdingRepo;
    private final AllocationAppliedLogRepository logRepo;

    @Transactional
    public void allocateBatch(AllocateHoldingsBatchReq req) {
        for (var item : req.items()) {
            if (logRepo.existsByAllocationEventId(item.allocationEventId())) {
                continue; // 멱등: 이미 적용
            }

            var holding = holdingRepo
                    .findByMemberAccountIdAndTicker(item.memberAccountId(), item.ticker())
                    .orElseGet(() -> StockHolding.builder()
                            .memberAccountId(item.memberAccountId())
                            .brokerageId(item.brokerageId())
                            .ticker(item.ticker())
                            .totalQuantity(0L)
                            .availableQuantity(0L)
                            .totalPurchasePrice(0L)
                            .build());

            long qty = item.quantity();
            long locked = item.lockedQuantity();
            long availInc = Math.subtractExact(qty, locked);

            holding.increaseTotalQuantity(qty);
            holding.increaseAvaQuantity(availInc);
            holding.incTotalPurchasePrice(qty, item.pricePerShare());

            holdingRepo.save(holding);
            logRepo.save(AllocationAppliedLog.builder()
                    .allocationEventId(item.allocationEventId()).build());
        }
    }
}
