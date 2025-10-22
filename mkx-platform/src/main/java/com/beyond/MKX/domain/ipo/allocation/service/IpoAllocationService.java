package com.beyond.MKX.domain.ipo.allocation.service;

import com.beyond.MKX.domain.account.brokerage.repository.BrokerageDepositAccountRepository;
import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.entity.IpoAllocationOutbox;
import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.entity.OutboxStatus;
import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.repository.IpoAllocationOutboxRepository;
import com.beyond.MKX.domain.ipo.allocation.dto.IpoAllocationSummaryResDTO;
import com.beyond.MKX.domain.ipo.allocation.entity.AllocationStatus;
import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import com.beyond.MKX.domain.ipo.allocation.repository.IpoAllocationRepository;
import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.ipo.entity.IpoStatus;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.ipo.subscription.entity.IpoSubscription;
import com.beyond.MKX.domain.ipo.subscription.entity.SubscriptionStatus;
import com.beyond.MKX.domain.ipo.subscription.repository.IpoSubscriptionRepository;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IpoAllocationService {
    private final IpoAllocationRepository allocationRepository;
    private final IpoSubscriptionRepository subscriptionRepository;
    private final IpoOfferingRepository offeringRepository;
    private final IpoAllocationOutboxRepository outboxRepository;
    private final Clock clock = Clock.systemDefaultZone();

    /**
     * 배정 확정 (배정만, 돈 처리/환불은 별도 단계)
     * - 전제: Offering은 PRICE_FIXED 상태, Subscriptions는 PAID만 대상
     * - 결과: IpoAllocation 생성/저장, Offering은 ALLOCATED 전환
     */
    @Transactional
    public IpoAllocationSummaryResDTO ipoAllocated(UUID offeringId) {
        long startNs = System.nanoTime();
        log.info("[IPO][START] ipoAllocated(offeringId={})", offeringId);

        // 1) 오퍼링 잠금 조회 & 상태 가드
        IpoOffering ipoOffering = offeringRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));

        log.info("[IPO] offering loaded. id={}, status={}, offerQty={}, offerPrice={}, roundNo={}, lotSize={}",
                ipoOffering.getId(), ipoOffering.getIpoOfferingStatus(),
                ipoOffering.getOfferQuantity(), ipoOffering.getOfferPrice(),
                ipoOffering.getRoundNo(), ipoOffering.getLotSize());

        if (ipoOffering.getIpoOfferingStatus() != IpoOfferingStatus.CLOSED) {
            log.warn("[IPO][GUARD] invalid offering status. required=CLOSED, actual={}",
                    ipoOffering.getIpoOfferingStatus());
            throw new IllegalArgumentException("배정은 CLOSED 상태에서만 가능합니다.");
        }

        Ipo ipo = Optional.ofNullable(ipoOffering.getIpo())
                .orElseThrow(() -> new IllegalStateException("Offering에 Ipo가 없습니다."));
        if (ipo.getStatus() != IpoStatus.APPROVED) {
            throw new IllegalArgumentException("IPO가 APPROVED 상태가 아닙니다.");
        }

        final long offerQuantity = nvl(ipoOffering.getOfferQuantity());
        if (offerQuantity <= 0) {
            log.warn("[IPO][GUARD] offerQuantity <= 0. offerQuantity={}", offerQuantity);
            throw new IllegalArgumentException("배정 가능 수량이 0 이하입니다.");
        }
        final long offerPrice = nvl(ipoOffering.getOfferPrice());
        final int roundNo = nvl(ipoOffering.getRoundNo(), 1);
        final Integer lotSize = ipoOffering.getLotSize();

        // 2) 대상 구독 조회(결정론적 정렬)
        List<IpoSubscription> subscriptions = subscriptionRepository
                .findAllByOfferingIdAndStatusOrderByPaid(offeringId, SubscriptionStatus.PAID);
        log.info("[IPO] subscriptions loaded. count={}", subscriptions.size());

        if (subscriptions.isEmpty()) {
            ipoOffering.allocated(0L);
            log.info("[IPO] no subscriptions. set ALLOCATED(0)");
            // push 생략, 요약 반환
            return IpoAllocationSummaryResDTO.of(ipoOffering, List.of(), 0L);
        }

        // 3) 총 수요
        long totalApplied = subscriptionRepository
                .sumAppliedQuantityByOffering(offeringId, SubscriptionStatus.PAID);
        log.info("[IPO] totalApplied={}", totalApplied);

        if (totalApplied <= 0) {
            ipoOffering.allocated(0L);                  // 배정 0으로 ALLOCATED 전이
            log.info("[IPO] totalApplied=0. set ALLOCATED(0)");
            return IpoAllocationSummaryResDTO.of(ipoOffering, List.of(), 0L);
        }

        // 4) 1차 비례배정 (lotSize 보정)
        Map<UUID, Long> allocationMap = new LinkedHashMap<>();
        long assigned = 0L;

        for (IpoSubscription s : subscriptions) {
            long applied = nvl(s.getAppliedQuantity());
            long raw = (offerQuantity == 0) ? 0 : (applied * offerQuantity) / totalApplied;
            if (lotSize != null && lotSize > 1) raw = (raw / lotSize) * lotSize;
            raw = Math.min(raw, applied);
            allocationMap.put(s.getId(), raw);
            assigned += raw;
        }
        log.info("[IPO] proportional allocation done. assigned={}, mapSize={}", assigned, allocationMap.size());

        // 5) 잔여 라운드로빈
        long remain = offerQuantity - assigned;
        long step = (lotSize != null && lotSize > 1) ? lotSize : 1L;
        int rrLoop = 0;

        while (remain >= step) {
            boolean gaveAny = false;
            for (IpoSubscription s : subscriptions) {
                long applied = nvl(s.getAppliedQuantity());
                long cur = allocationMap.getOrDefault(s.getId(), 0L);
                long canGive = Math.min(step, applied - cur);
                if (canGive >= step) {
                    allocationMap.put(s.getId(), cur + step);
                    remain -= step;
                    gaveAny = true;
                    if (remain < step) break;
                }
            }
            rrLoop++;
            if (!gaveAny) break;
        }
        log.info("[IPO] round-robin done. loops={}, remain={}", rrLoop, remain);

        // 6) Allocation 생성(멱등 가드)
        var now = LocalDateTime.now(clock);
        List<IpoAllocation> toSave = new ArrayList<>();
        int skippedExists = 0, skippedNotPaid = 0, zeroQty = 0;

        for (IpoSubscription s : subscriptions) {
            long qty = allocationMap.getOrDefault(s.getId(), 0L);
            if (qty <= 0) {
                zeroQty++;
                continue;
            }
            if (s.getStatus() != SubscriptionStatus.PAID) {
                skippedNotPaid++;
                continue;
            }
            if (allocationRepository.existsByIpoSubscription_IdAndRoundNo(s.getId(), roundNo)) {
                skippedExists++;
                continue;
            }
            toSave.add(IpoAllocation.of(s, qty, offerPrice, roundNo, now));
        }
        allocationRepository.saveAll(toSave);
        
        // 배정 완료 상태로 변경
        toSave.forEach(IpoAllocation::markCompleted);
        allocationRepository.saveAll(toSave);
        
        List<IpoAllocationOutbox> events = toSave.stream().map(a ->
                IpoAllocationOutbox.builder()
                        .idempotencyKey(a.getId())                           // = allocationId
                        .allocationId(a.getId())
                        .offeringId(ipoOffering.getId())
                        .ipoId(ipoOffering.getIpo().getId())
                        .accountId(a.getIpoSubscription().getAccountId())
                        .brokerageId(a.getIpoSubscription().getBrokerageId())
                        .qty(a.getAllocatedQuantity())
                        .offerPriceSnapshot(ipoOffering.getOfferPrice())     // 선택
                        .status(OutboxStatus.PENDING)
                        .build()
        ).toList();
        outboxRepository.saveAll(events);
        log.info("[IPO] allocations persisted. toSaveCount={}, skippedExists={}, skippedNotPaid={}, zeroQty={}",
                toSave.size(), skippedExists, skippedNotPaid, zeroQty);
        /* = =  = = = = = = ===========================================================================*/
        List<IpoAllocation> all = allocationRepository.findAllByOfferingId(offeringId);
        long totalAllocated = all.stream().mapToLong(IpoAllocation::getAllocatedQuantity).sum();

        // 이미 과거에 만들어둔 배정이 있으면(=totalAllocated > 0) ALLOCATED로 전환
        if (totalAllocated > 0 && ipoOffering.getIpoOfferingStatus() == IpoOfferingStatus.CLOSED) {
            ipoOffering.allocated(totalAllocated);
            log.info("[IPO] offering status -> ALLOCATED. totalAllocated={}", totalAllocated);
        } else {
            log.info("[IPO] no allocation in DB. keep CLOSED.");
        }
        return IpoAllocationSummaryResDTO.of(ipoOffering, all, totalAllocated);
    }

    private long nvl(Long v) {
        return v == null ? 0L : v;
    }

    private int nvl(Integer v, int def) {
        return v == null ? def : v;
    }


//    @Transactional
//    public IpoAllocationSummaryResDTO allocateAndSummarize(UUID offeringId) {
//        // 기존 배정 로직 재사용(상태 가드/배정 생성/오퍼링 상태 전환)
//
//        ipoAllocated(offeringId);
//
//        // DTO 조립은 서비스에서
//        IpoOffering offering = offeringRepository.findById(offeringId)
//                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));
//        List<IpoAllocation> list = allocationRepository.findAllByOfferingId(offeringId);
//        long allocatedTotalQuantity = list.stream().mapToLong(IpoAllocation::getAllocatedQuantity).sum();
//
//        return IpoAllocationSummaryResDTO.of(offering, list, allocatedTotalQuantity);
//    }

    @Transactional(readOnly = true)
    public IpoAllocationSummaryResDTO summarize(UUID offeringId) {
        IpoOffering o = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));

        // N+1 방지: fetch join 레포지토리 메서드 권장
        List<IpoAllocation> list = allocationRepository.findAllByOfferingId(offeringId);

        long allocatedTotalQuantity = list.stream()
                .mapToLong(IpoAllocation::getAllocatedQuantity)
                .sum();

        return IpoAllocationSummaryResDTO.of(o, list, allocatedTotalQuantity);
    }

    /**
     * 구독 단건 배정 완료
     * @param subscriptionId 구독 ID
     */
    @Transactional
    public void completeAllocationBySubscription(UUID subscriptionId) {
        log.info("[IPO][START] completeAllocationBySubscription(subscriptionId={})", subscriptionId);
        
        // 해당 구독의 최신 배정 조회
        IpoAllocation allocation = allocationRepository.findTopByIpoSubscription_IdOrderByRoundNoDesc(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 구독의 배정을 찾을 수 없습니다."));
        
        // 이미 완료된 배정인지 확인
        if (allocation.getStatus() == AllocationStatus.COMPLETED) {
            throw new IllegalArgumentException("이미 완료된 배정입니다.");
        }
        
        // 배정 완료 상태로 변경
        allocation.markCompleted();
        allocationRepository.save(allocation);
        
        log.info("[IPO][COMPLETE] completeAllocationBySubscription completed. allocationId={}", allocation.getId());
    }

}
