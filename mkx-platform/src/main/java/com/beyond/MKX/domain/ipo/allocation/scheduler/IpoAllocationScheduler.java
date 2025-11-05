package com.beyond.MKX.domain.ipo.allocation.scheduler;

import com.beyond.MKX.domain.ipo.allocation.service.IpoAllocationService;
import com.beyond.MKX.domain.ipo.allocation.service.IpoSettlementService;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 공모 배정 및 환불 자동 처리 스케줄러
 * - 배정예정일(allocationDate)에 배정 실행
 * - 환불일(refundDate)에 환불/정산 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpoAllocationScheduler {

    private final IpoOfferingRepository offeringRepository;
    private final IpoAllocationService allocationService;
    private final IpoSettlementService settlementService;
    private final Clock clock;

    /**
     * 배정예정일이 오늘인 공모들을 자동으로 배정 처리
     * - CLOSED 상태의 공모만 처리 (배정은 CLOSED 상태에서만 가능)
     * - 매일 오전 9시에 실행
     */
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시
    @Transactional
    public void autoExecuteAllocation() {
        LocalDate today = LocalDate.now(clock);
        log.info("[AUTO-ALLOCATION] 배정예정일({})인 공모 배정 자동 실행 시작", today);

        // 배정 가능한 상태: CLOSED (배정은 CLOSED 상태에서만 가능)
        List<IpoOfferingStatus> allocatableStatuses = Arrays.asList(
                IpoOfferingStatus.CLOSED
        );

        List<IpoOffering> offeringsToAllocate = offeringRepository
                .findAllByAllocationDateAndStatusIn(today, allocatableStatuses);

        if (offeringsToAllocate.isEmpty()) {
            log.info("[AUTO-ALLOCATION] 배정예정일({})인 공모가 없습니다.", today);
            return;
        }

        log.info("[AUTO-ALLOCATION] 배정 대상 공모 {}건 발견", offeringsToAllocate.size());

        for (IpoOffering offering : offeringsToAllocate) {
            try {
                log.info("[AUTO-ALLOCATION] 공모 {} 배정 실행 시작 (symbol={}, status={})",
                        offering.getId(), offering.getIpo().getSymbol(), offering.getIpoOfferingStatus());

                // 배정 실행
                allocationService.ipoAllocated(offering.getId());

                log.info("[AUTO-ALLOCATION] ✅ 공모 {} 배정 완료 (symbol={})",
                        offering.getId(), offering.getIpo().getSymbol());

            } catch (Exception e) {
                log.error("[AUTO-ALLOCATION] ❌ 공모 {} 배정 실패 (symbol={}): {}",
                        offering.getId(), offering.getIpo().getSymbol(), e.getMessage(), e);
                // 실패해도 다음 공모는 계속 처리
            }
        }

        log.info("[AUTO-ALLOCATION] 배정예정일({})인 공모 배정 자동 실행 완료 (처리: {}건)",
                today, offeringsToAllocate.size());
    }

    /**
     * 환불일이 오늘인 공모들을 자동으로 환불/정산 처리
     * - ALLOCATED 상태의 공모만 처리
     * - 매일 오전 10시에 실행 (배정 후 1시간 여유)
     */
    @Scheduled(cron = "0 0 10 * * *") // 매일 오전 10시
    @Transactional
    public void autoExecuteRefund() {
        LocalDate today = LocalDate.now(clock);
        log.info("[AUTO-REFUND] 환불일({})인 공모 환불/정산 자동 실행 시작", today);

        List<IpoOffering> offeringsToRefund = offeringRepository
                .findAllByRefundDateAndStatus(today, IpoOfferingStatus.ALLOCATED);

        if (offeringsToRefund.isEmpty()) {
            log.info("[AUTO-REFUND] 환불일({})인 공모가 없습니다.", today);
            return;
        }

        log.info("[AUTO-REFUND] 환불 대상 공모 {}건 발견", offeringsToRefund.size());

        for (IpoOffering offering : offeringsToRefund) {
            try {
                log.info("[AUTO-REFUND] 공모 {} 환불/정산 실행 시작 (symbol={})",
                        offering.getId(), offering.getIpo().getSymbol());

                // 전체 정산 실행 (환불 포함)
                settlementService.settleAllPaymentsByOffering(offering.getId());

                log.info("[AUTO-REFUND] ✅ 공모 {} 환불/정산 완료 (symbol={})",
                        offering.getId(), offering.getIpo().getSymbol());

            } catch (Exception e) {
                log.error("[AUTO-REFUND] ❌ 공모 {} 환불/정산 실패 (symbol={}): {}",
                        offering.getId(), offering.getIpo().getSymbol(), e.getMessage(), e);
                // 실패해도 다음 공모는 계속 처리
            }
        }

        log.info("[AUTO-REFUND] 환불일({})인 공모 환불/정산 자동 실행 완료 (처리: {}건)",
                today, offeringsToRefund.size());
    }
}

