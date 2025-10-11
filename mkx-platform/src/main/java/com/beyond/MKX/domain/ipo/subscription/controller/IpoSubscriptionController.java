package com.beyond.MKX.domain.ipo.subscription.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.ipo.subscription.dto.IpoSubscriptionReqDTO;
import com.beyond.MKX.domain.ipo.subscription.dto.IpoSubscriptionResDTO;
import com.beyond.MKX.domain.ipo.subscription.dto.DepositReqDTO;
import com.beyond.MKX.domain.ipo.subscription.service.IpoSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    /** 청약 취소 (APPLIED -> CANCELLED) */
    @PatchMapping("/{subscriptionId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable UUID subscriptionId) {
        IpoSubscriptionResDTO res = subscriptionService.cancel(subscriptionId);
        return ApiResponse.ok(res, "청약이 취소되었습니다.");
    }

    /** 단건 조회 (선택) */
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<?> get(@PathVariable UUID subscriptionId) {
        IpoSubscriptionResDTO res = subscriptionService.get(subscriptionId);
        return ApiResponse.ok(res, "조회 완료");
    }

    /** 청약 납입(APPLIED -> PAID) / 향후 시간이 된다면 추가공모로직으로 쓸 예정 / 아마 못 쓸 듯, 그래도 아까워서  */
//    @PatchMapping("/{subscriptionId}/deposit")
//    public ResponseEntity<?> paid(@PathVariable UUID subscriptionId, @Valid @RequestBody DepositReqDTO reqDTO) {
//        IpoSubscriptionResDTO subscriptionRes = subscriptionService.deposit(subscriptionId, reqDTO.getDepositAmount());
//        return ApiResponse.ok(subscriptionRes, "증거금 납입 처리가 완료되었습니다.");
//    }
}
