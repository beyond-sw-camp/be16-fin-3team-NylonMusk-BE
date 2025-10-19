package com.beyond.MKX.domain.ipo.allocation.service;

import com.beyond.MKX.domain.ipo.allocation.dto.IpoAllocationSummaryResDTO;
import com.beyond.MKX.domain.ipo.allocation.dto.StockUpdateDTO;
import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import com.beyond.MKX.domain.ipo.allocation.repository.IpoAllocationRepository;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.ipo.subscription.entity.IpoSubscription;
import com.beyond.MKX.domain.ipo.subscription.entity.SubscriptionStatus;
import com.beyond.MKX.domain.ipo.subscription.repository.IpoSubscriptionRepository;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IpoAllocationService {
    private final IpoAllocationRepository allocationRepository;
    private final IpoSubscriptionRepository subscriptionRepository;
    private final IpoOfferingRepository offeringRepository;
    private final StockRepository stockRepository;
    private final IpoAllocationFeign ipoAllocationFeign;
    private final Clock clock = Clock.systemDefaultZone();

    /**
     * 배정 확정 (배정만, 돈 처리/환불은 별도 단계)
     * - 전제: Offering은 PRICE_FIXED 상태, Subscriptions는 PAID만 대상
     * - 결과: IpoAllocation 생성/저장, Offering은 ALLOCATED 전환
     */
    @Transactional
    public UUID ipoAllocated(UUID offeringId) {
        // 1) 오퍼링 잠금 조회 & 상태 가드
        IpoOffering ipoOffering = offeringRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));

        if (ipoOffering.getIpoOfferingStatus() != IpoOfferingStatus.CLOSED) {
            throw new IllegalArgumentException("배정은 CLOSED 상태에서만 가능합니다.");
        }
        final long offerQuantity = nvl(ipoOffering.getOfferQuantity());
        if (offerQuantity <= 0) {
            throw new IllegalArgumentException("배정 가능 수량이 0 이하입니다.");
        }
        final long offerPrice = nvl(ipoOffering.getOfferPrice());
        final int roundNo = nvl(ipoOffering.getRoundNo(), 1);
        final Integer lotSize = ipoOffering.getLotSize();

        // 2) 대상 구독 조회(결정론적 정렬)
        List<IpoSubscription> subscriptions = subscriptionRepository
                .findAllByOfferingIdAndStatusOrderByPaid(offeringId, SubscriptionStatus.PAID);
//        공모 청약이 0인 경우
        if (subscriptions.isEmpty()) {
            ipoOffering.allocated(0L);
            return offeringId;
        }

        // 3) 총 수요
        long totalApplied = subscriptionRepository.sumAppliedQuantityByOffering(offeringId, SubscriptionStatus.PAID);

//        수요가 0인 경우
        if (totalApplied <= 0) {
            ipoOffering.allocated(0L);
            return offeringId;
        }

        // 4) 1차 비례배정 (lotSize 배수 보정)
        Map<UUID, Long> allocationMap = new LinkedHashMap<>();
        long assigned = 0L;
        for (IpoSubscription s : subscriptions) {
            long applied = nvl(s.getAppliedQuantity());
            long raw = (offerQuantity == 0) ? 0 : (applied * offerQuantity) / totalApplied;
            if (lotSize != null && lotSize > 1) {
                raw = (raw / lotSize) * lotSize;
            }
            raw = Math.min(raw, applied);
            allocationMap.put(s.getId(), raw);
            assigned += raw;
        }

        // 5) 잔여 주식 라운드로빈 (lotSize 단위)
        long remain = offerQuantity - assigned;
        long step = (lotSize != null && lotSize > 1) ? lotSize : 1L;

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
            if (!gaveAny) break; // 더 줄 곳 없으면 종료
        }

        // 6) Allocation 생성(멱등 가드)
        var now = LocalDateTime.now(clock);
        List<IpoAllocation> toSave = new ArrayList<>();
        for (IpoSubscription s : subscriptions) {
            long qty = allocationMap.getOrDefault(s.getId(), 0L);
            if (qty <= 0) continue;
            if (s.getStatus() != SubscriptionStatus.PAID) continue;
            if (allocationRepository.existsByIpoSubscription_IdAndRoundNo(s.getId(), roundNo)) continue;

            toSave.add(IpoAllocation.of(s, qty, offerPrice, roundNo, now));
        }
        allocationRepository.saveAll(toSave);

        // 여기 추가 (배정 수량 > 0인 구독만 ALLOCATED로 전환)
        toSave.forEach(a -> {
            IpoSubscription s = a.getIpoSubscription(); // 영속 상태(Managed) 엔티티
            if (s.getStatus() == SubscriptionStatus.PAID) {
                s.setStatus(SubscriptionStatus.ALLOCATED);
            }
        });

        subscriptionRepository.flush();

        // 커밋 이후 전송
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                final String ticker = resolveTickerCodeOrThrow(ipoOffering);
                final Long offerPrice = nvl(ipoOffering.getOfferPrice());

                for (IpoAllocation a : toSave) {
                    IpoSubscription s = a.getIpoSubscription();

                    StockUpdateDTO dto = StockUpdateDTO.builder()
                            .idempotencyKey(a.getId())                 // 멱등키 = allocationId 재사용
                            .allocationId(a.getId())
                            .offeringId(ipoOffering.getId())           // 선택
                            .memberAccountId(s.getAccountId())   // 계좌 ID
                            .ticker(ticker)
                            .qtyDelta(a.getAllocatedQuantity())
                            .unitPrice(offerPrice)
                            .reason("IPO_ALLOCATION")
                            .build();

                    ipoAllocationFeign.applyStockUpdate(dto);
                }
            }
        });

        // 7) 배정 총합으로 상태 전환 (0도 허용)
        long assignedFinal = toSave.stream().mapToLong(IpoAllocation::getAllocatedQuantity).sum();
        ipoOffering.allocated(assignedFinal);

        return offeringId;
    }

    private long nvl(Long v) {
        return v == null ? 0L : v;
    }

    private int nvl(Integer v, int def) {
        return v == null ? def : v;
    }


    /**
     * 종목코드(ticker) 해석
     * - Ipo.symbol(종목명) 아님!
     * - corporationId + LISTED로 현재 상장된 Stock의 ticker를 얻음
     */
    private String resolveTickerCodeOrThrow(IpoOffering offering) {
        var ipo = Optional.ofNullable(offering.getIpo())
                .orElseThrow(() -> new IllegalStateException("Offering에 Ipo가 없습니다."));
        var corpId = ipo.getCorporation().getId();

        return stockRepository.findListedTickerByCorporationId(corpId, Stock.Status.LISTED)
                .orElseThrow(() -> new IllegalStateException("LISTED 상태 Stock의 종목코드가 없습니다. 상장/Stock 생성 확인 필요"));
    }

    @Transactional
    public IpoAllocationSummaryResDTO allocateAndSummarize(UUID offeringId) {
        // 기존 배정 로직 재사용(상태 가드/배정 생성/오퍼링 상태 전환)
        ipoAllocated(offeringId);

        // DTO 조립은 서비스에서
        IpoOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));
        List<IpoAllocation> list = allocationRepository.findAllByOfferingId(offeringId);
        long allocatedTotalQuantity = list.stream().mapToLong(IpoAllocation::getAllocatedQuantity).sum();

        return IpoAllocationSummaryResDTO.of(offering, list, allocatedTotalQuantity);
    }

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

}
