package com.beyond.MKX.domain.ipo.allocation.controller;


import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import com.beyond.MKX.domain.ipo.allocation.repository.IpoAllocationRepository;
import com.beyond.MKX.domain.ipo.allocation.service.IpoAllocationService;
import com.beyond.MKX.domain.ipo.allocation.service.IpoSettlementService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ipo")
public class IpoAllocationController {
    private final IpoAllocationService allocationService;
    private final IpoSettlementService settlementService;
    private final IpoAllocationRepository allocationRepository;

    /* 1) 배정 실행 (PRICE_FIXED 상태에서만) */
    @PostMapping("/offerings/{offeringId}/allocate")
    public ResponseEntity<?> allocate(@PathVariable @NotNull UUID offeringId) {
        UUID id = allocationService.ipoAllocated(offeringId);
        return ApiResponse.ok(id, "배정을 완료했습니다.");
    }

    /* 2) 구독 단건 정산 (추가납입/환불) — CORPORATION 우선 지원 */
    @PostMapping("/subscriptions/{subscriptionId}/settle")
    public ResponseEntity<?> settleBySubscription(@PathVariable @NotNull UUID subscriptionId) {
        UUID id = settlementService.settlePaymentsBySubscription(subscriptionId);
        return ApiResponse.ok(id, "해당 청약 건의 추가납입/환불 정산을 완료했습니다.");
    }

    /* 3) 발행사로 공모 대금 송금 (ALLOCATED/PRICE_FIXED 허용, 내부 검증 포함) */
    @PostMapping("/offerings/{offeringId}/payout")
    public ResponseEntity<?> payoutToIssuer(@PathVariable @NotNull UUID offeringId) {
        UUID id = settlementService.payoutOfferingToIssuer(offeringId);
        return ApiResponse.ok(id, "발행사 송금을 완료하고 공모를 정산(SETTLED) 처리했습니다.");
    }

    /* 4) (옵션) 특정 공모의 배정 목록 조회 — 운영 점검/관리자용 */
    @GetMapping("/offerings/{offeringId}/allocations")
    public ResponseEntity<?> findAllocationsOfOffering(@PathVariable @NotNull UUID offeringId) {
        List<IpoAllocation> list = allocationRepository.findAllByOfferingId(offeringId);
        return ApiResponse.ok(list, "해당 공모의 배정 목록입니다.");
    }

    /* 5) (옵션) 특정 구독의 최신 배정 조회 — 클라이언트 확인용 */
    @GetMapping("/subscriptions/{subscriptionId}/allocation/latest")
    public ResponseEntity<?> findLatestAllocationOfSubscription(@PathVariable @NotNull UUID subscriptionId) {
        var latest = allocationRepository.findTopByIpoSubscription_IdOrderByRoundNoDesc(subscriptionId)
                .orElse(null);
        return ApiResponse.ok(latest, "해당 청약의 최신 배정 결과입니다.");
    }
}
