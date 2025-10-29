package com.beyond.MKX.domain.ipo.subscription.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.common.auth.security.CustomMemberPrincipal;
import com.beyond.MKX.domain.ipo.subscription.dto.IpoSubscriptionReqDTO;
import com.beyond.MKX.domain.ipo.subscription.dto.IpoSubscriptionResDTO;
import com.beyond.MKX.domain.ipo.subscription.dto.DepositReqDTO;
import com.beyond.MKX.domain.ipo.subscription.entity.InvestorType;
import com.beyond.MKX.domain.ipo.subscription.service.IpoSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/ipo/subscription")
@RequiredArgsConstructor
public class IpoSubscriptionController {
    private final IpoSubscriptionService subscriptionService;

    /* 청약 신청(APPLY) & 증거금 납입 (PAID) 원큐 */
    @PostMapping
    public ResponseEntity<?> apply(@Valid @RequestBody IpoSubscriptionReqDTO reqDTO) {
        IpoSubscriptionResDTO subscriptionRes = subscriptionService.apply(reqDTO);
        return ApiResponse.ok(subscriptionRes, "청약 접수가 완료되었습니다.");
    }

    /**
     * 청약 취소 (APPLIED or PAID -> CANCELLED)
     */
    @PatchMapping("/{subscriptionId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable UUID subscriptionId) {
        IpoSubscriptionResDTO res = subscriptionService.cancel(subscriptionId);
        return ApiResponse.ok(res, "청약이 취소되었습니다.");
    }

    /**
     * 단건 조회 (선택)
     */
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<?> get(@PathVariable UUID subscriptionId) {
        IpoSubscriptionResDTO res = subscriptionService.get(subscriptionId);
        return ApiResponse.ok(res, "조회 완료");
    }

    // 특정 공모의 청약 전체 조회
    @GetMapping("/{offeringId}/list")
    public ResponseEntity<?> getAll(@PathVariable UUID offeringId) {
        List<IpoSubscriptionResDTO> res = subscriptionService.findAll(offeringId);
        return ApiResponse.ok(res, "해당 공모의 청약 리스트입니다.");
    }

    /** 개인 투자자 본인 청약 내역 조회 */
    @GetMapping("/member")
    public ResponseEntity<?> getMySubscriptionsForMember(@AuthenticationPrincipal CustomMemberPrincipal principal) {
        UUID memberId = principal.id();
        List<IpoSubscriptionResDTO> res = subscriptionService.findAllBySubscriber(memberId, InvestorType.INDIVIDUAL);
        return ApiResponse.ok(res, "개인 투자자의 청약 내역입니다.");
    }

    /** 기업 투자자 본인 청약 내역 조회 */
    @GetMapping("/corporation")
    public ResponseEntity<?> getMySubscriptionsForCorporation(@AuthenticationPrincipal CustomAdminPrincipal principal) {
        UUID corporationId = principal.id();
        List<IpoSubscriptionResDTO> res = subscriptionService.findAllBySubscriber(corporationId, InvestorType.CORPORATION);
        return ApiResponse.ok(res, "기업 투자자의 청약 내역입니다.");
    }

    /** 청약 납입(APPLIED -> PAID) / 향후 시간이 된다면 추가공모로직으로 쓸 예정 / 아마 못 쓸 듯, 그래도 아까워서  */
//    @PatchMapping("/{subscriptionId}/deposit")
//    public ResponseEntity<?> paid(@PathVariable UUID subscriptionId, @Valid @RequestBody DepositReqDTO reqDTO) {
//        IpoSubscriptionResDTO subscriptionRes = subscriptionService.deposit(subscriptionId, reqDTO.getDepositAmount());
//        return ApiResponse.ok(subscriptionRes, "증거금 납입 처리가 완료되었습니다.");
//    }
}
