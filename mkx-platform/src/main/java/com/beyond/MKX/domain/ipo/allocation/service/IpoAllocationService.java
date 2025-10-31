package com.beyond.MKX.domain.ipo.allocation.service;

import com.beyond.MKX.domain.account.brokerage.repository.BrokerageDepositAccountRepository;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.entity.IpoAllocationOutbox;
import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.entity.OutboxStatus;
import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.repository.IpoAllocationOutboxRepository;
import com.beyond.MKX.domain.ipo.allocation.dto.IpoAllocationItemResDTO;
import com.beyond.MKX.domain.ipo.allocation.dto.IpoAllocationSummaryResDTO;
import com.beyond.MKX.domain.ipo.allocation.entity.AllocationStatus;
import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import com.beyond.MKX.domain.ipo.allocation.repository.IpoAllocationRepository;
import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.ipo.entity.IpoStatus;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingType;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.ipo.subscription.dto.IpoSubscriptionResDTO;
import com.beyond.MKX.domain.ipo.subscription.entity.IpoSubscription;
import com.beyond.MKX.domain.ipo.subscription.entity.SubscriptionStatus;
import com.beyond.MKX.domain.ipo.subscription.entity.InvestorType;
import com.beyond.MKX.domain.ipo.subscription.repository.IpoSubscriptionRepository;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IpoAllocationService {
    private final IpoAllocationRepository allocationRepository;
    private final IpoSubscriptionRepository subscriptionRepository;
    private final IpoOfferingRepository offeringRepository;
    private final IpoAllocationOutboxRepository outboxRepository;
    private final CorporationRepository corporationRepository;
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
        // ──────────────────────────────────────────────
        // 1) 오퍼링 잠금 조회 & 상태 가드
        // ──────────────────────────────────────────────
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

        // ──────────────────────────────────────────────
        // 2) 신규 공모와 N차 공모 분기
        // ──────────────────────────────────────────────
        if (ipoOffering.getOfferingType() == IpoOfferingType.INITIAL) {
            // IPO(최초 상장) 공모는 반드시 APPROVED 필요
            if (ipo.getStatus() != IpoStatus.APPROVED) {
                throw new IllegalArgumentException("IPO가 APPROVED 상태가 아닙니다. (신규 상장)");
            }
        } else {
            // FOLLOW_ON / RIGHTS_ISSUE는 이미 LISTED 상태면 허용
            if (ipo.getStatus() != IpoStatus.LISTED) {
                throw new IllegalArgumentException("N차 공모는 상장된(LISTED) 종목에서만 가능합니다.");
            }
        }

        final long offerQuantity = nvl(ipoOffering.getOfferQuantity());
        if (offerQuantity <= 0) {
            log.warn("[IPO][GUARD] offerQuantity <= 0. offerQuantity={}", offerQuantity);
            throw new IllegalArgumentException("배정 가능 수량이 0 이하입니다.");
        }
        final long offerPrice = nvl(ipoOffering.getOfferPrice());
        final int roundNo = nvl(ipoOffering.getRoundNo(), 1);
        final Integer lotSize = ipoOffering.getLotSize();

        // ──────────────────────────────────────────────
        // 3) 구독 조회(결정론적 정렬)
        // ──────────────────────────────────────────────
        List<IpoSubscription> subscriptions = subscriptionRepository
                .findAllByOfferingIdAndStatusOrderByPaid(offeringId, SubscriptionStatus.PAID);
        log.info("[IPO] subscriptions loaded. count={}", subscriptions.size());

        if (subscriptions.isEmpty()) {
            ipoOffering.allocated(0L);
            log.info("[IPO] no subscriptions. set ALLOCATED(0)");
            // push 생략, 요약 반환
            return IpoAllocationSummaryResDTO.of(ipoOffering, List.of(), 0L);
        }

        // ──────────────────────────────────────────────
        // 4) 총 수요량 계산 및 기본 배분
        // ──────────────────────────────────────────────
        long totalApplied = subscriptionRepository
                .sumAppliedQuantityByOffering(offeringId, SubscriptionStatus.PAID);
        log.info("[IPO] totalApplied={}", totalApplied);

        if (totalApplied <= 0) {
            ipoOffering.allocated(0L);                  // 배정 0으로 ALLOCATED 전이
            log.info("[IPO] totalApplied=0. set ALLOCATED(0)");
            return IpoAllocationSummaryResDTO.of(ipoOffering, List.of(), 0L);
        }

        // 비례배정 계산
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

        // ──────────────────────────────────────────────
        // 5) 잔여 라운드로빈
        // ──────────────────────────────────────────────
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

        // ──────────────────────────────────────────────
        // 6) Allocation 생성(멱등 가드)
        // ──────────────────────────────────────────────
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

        // === 배정된 구독(subscription_status) 상태를 ALLOCATED로 전환 ===
        for (IpoSubscription s : subscriptions) {
            long qty = allocationMap.getOrDefault(s.getId(), 0L);
            if (qty > 0 && s.getStatus() == SubscriptionStatus.PAID) {
                s.setStatus(SubscriptionStatus.ALLOCATED);
            }
        }
        subscriptionRepository.saveAll(subscriptions);

        // 배정 완료 상태로 변경
        toSave.forEach(IpoAllocation::markCompleted);
        allocationRepository.saveAll(toSave);

        // OutBox에 기록
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

        // ──────────────────────────────────────────────
        // 7) OFFERING 상태 전환
        // ──────────────────────────────────────────────
        List<IpoAllocation> all = allocationRepository.findAllByOfferingId(offeringId);
        long totalAllocated = all.stream().mapToLong(IpoAllocation::getAllocatedQuantity).sum();

        // 이미 과거에 만들어둔 배정이 있으면(=totalAllocated > 0) ALLOCATED로 전환
        if (totalAllocated > 0 && ipoOffering.getIpoOfferingStatus() == IpoOfferingStatus.CLOSED) {
            if (ipoOffering.getOfferingType() == IpoOfferingType.FOLLOW_ON
                    || ipoOffering.getOfferingType() == IpoOfferingType.RIGHTS_ISSUE) {
                // N차 공모 -> 거래소 승인 대기 상태로 전환
                ipoOffering.setIpoOfferingStatus(IpoOfferingStatus.ALLOCATION_PENDING);
                log.info("[IPO] N차 공모 배정 완료 → 거래소 승인 대기(ALLOCATION_PENDING)");
            } else {
                // 신규 공모
                ipoOffering.allocated(totalAllocated);
                log.info("[IPO] 신규공모 배정 완료 → ALLOCATED");
            }
            offeringRepository.save(ipoOffering);
        }
        return IpoAllocationSummaryResDTO.of(ipoOffering, all, totalAllocated);
    }

    private long nvl(Long v) { return v == null ? 0L : v; }

    private int nvl(Integer v, int def) { return v == null ? def : v; }

    /**
     * 거래소 관리자 N차 공모 & 유상증자 승인 처리
     * - ALLOCATION_PENDING → ALLOCATED 전환
     */
    @Transactional
    public IpoAllocationSummaryResDTO approveAllocation(UUID offeringId) {
        log.info("[IPO][ADMIN] approveAllocation(offeringId={})", offeringId);

        // 1️⃣ 공모 엔티티를 조회 (SELECT ... FOR UPDATE)
        //    → 동시 승인 방지를 위해 DB 락을 겁니다.
        IpoOffering offering = offeringRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));

        // 2️⃣ 상태 검증: 거래소 승인 대기 상태여야 함
        if (offering.getIpoOfferingStatus() != IpoOfferingStatus.ALLOCATION_PENDING) {
            throw new IllegalStateException("승인 대기 상태(ALLOCATION_PENDING)인 공모만 승인할 수 있습니다.");
        }

        // 3️⃣ 배정 내역 조회: 이미 생성된 IpoAllocation 목록을 DB에서 가져옴
        List<IpoAllocation> allocations = allocationRepository.findAllByOfferingId(offeringId);

        // 4️⃣ 총 배정 수량 합계 계산
        long totalAllocated = allocations.stream()
                .mapToLong(IpoAllocation::getAllocatedQuantity)
                .sum();

        // 거래소 검증 완료 상태로 전환 (ALLOCATED 아님!)
        offering.setIpoOfferingStatus(IpoOfferingStatus.VERIFIED);
        // → save(offering) 시점 또는 트랜잭션 커밋 시 DB에 UPDATE 쿼리 반영됨.
        offeringRepository.save(offering);

        log.info("[IPO][ADMIN] 거래소 승인 완료 → VERIFIED 전환 (totalAllocated={})", totalAllocated);

        // 7️⃣ 배정 결과를 DTO로 감싸서 반환
        return IpoAllocationSummaryResDTO.of(offering, allocations, totalAllocated);
    }

    // 발행사 배정 확정 메서드
    @Transactional
    public IpoAllocationSummaryResDTO confirmAllocation(UUID offeringId) {
        log.info("[IPO][ISSUER] confirmAllocation(offeringId={})", offeringId);

        IpoOffering offering = offeringRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));

        if (offering.getIpoOfferingStatus() != IpoOfferingStatus.VERIFIED) {
            throw new IllegalStateException("거래소 검증(VERIFIED) 이후만 확정 가능합니다.");
        }

        List<IpoAllocation> allocations = allocationRepository.findAllByOfferingId(offeringId);
        long totalAllocated = allocations.stream()
                .mapToLong(IpoAllocation::getAllocatedQuantity)
                .sum();

        offering.allocated(totalAllocated);  // 내부에서 ALLOCATED 세팅
        offeringRepository.save(offering);

        log.info("[IPO][ISSUER] 발행사 배정 확정 완료 → ALLOCATED");

        return IpoAllocationSummaryResDTO.of(offering, allocations, totalAllocated);
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

        IpoAllocationSummaryResDTO result = IpoAllocationSummaryResDTO.of(o, list, allocatedTotalQuantity);

        // 투자자 이름 설정
        result.getAllocations().forEach(item -> {
            String subscriberName = resolveSubscriberNameForCorporationView(item);
            log.info("[IPO] Setting subscriber name for item: subscriberId={}, investorType={}, subscriberName={}",
                    item.getSubscriberId(), item.getInvestorType(), subscriberName);
            item.setSubscriberName(subscriberName);
        });

        return result;
    }

    /**
     * 구독 단건 배정 완료
     *
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

    /**
     * 발행사 관점: 기관만 실명, 개인은 익명 처리
     */
    private String resolveSubscriberNameForCorporationView(IpoAllocationItemResDTO item) {
        log.info("[IPO] resolveSubscriberNameForCorporationView: subscriberId={}, investorType={}",
                item.getSubscriberId(), item.getInvestorType());

        try {
            if (item.getInvestorType() != null &&
                    (item.getInvestorType().equals("CORPORATION") || item.getInvestorType().equals(InvestorType.CORPORATION.name()))) {

                log.info("[IPO] Processing CORPORATION investor: subscriberId={}", item.getSubscriberId());
                UUID subscriberId = UUID.fromString(item.getSubscriberId());
                String corporationName = corporationRepository.findById(subscriberId)
                        .map(c -> c.getNameKo())
                        .orElse("알 수 없음");
                log.info("[IPO] Resolved corporation name: {}", corporationName);
                return corporationName;
            }
            // 개인투자자는 이름 노출 금지
            log.info("[IPO] Processing INDIVIDUAL investor or unknown type.");
            return "개인 투자자";
        } catch (IllegalArgumentException e) {
            log.error("[IPO] Invalid UUID format for subscriberId: {}", item.getSubscriberId(), e);
            return "알 수 없음 (UUID 오류)";
        } catch (Exception e) {
            log.error("[IPO] Error resolving subscriber name for subscriberId={}: {}", item.getSubscriberId(), e.getMessage(), e);
            return "알 수 없음 (처리 오류)";
        }
    }

}
