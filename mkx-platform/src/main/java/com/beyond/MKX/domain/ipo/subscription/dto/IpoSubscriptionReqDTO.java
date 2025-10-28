package com.beyond.MKX.domain.ipo.subscription.dto;

import com.beyond.MKX.domain.ipo.subscription.entity.InvestorType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record IpoSubscriptionReqDTO(
        @NotNull
        UUID ipoOfferingId,
        @NotNull
        InvestorType investorType,   // INDIVIDUAL | CORPORATION
        @NotNull
        UUID subscriberId,           // 개인/법인 식별자
        @NotNull
        UUID brokerageId,            // 경유 증권사
        @NotNull
        UUID accountId,              // 납입/환불 계좌

        String accountNumber,   // 개인용: MemberAccount용 문자열
        @NotNull @Min(1)
        Long appliedQuantity // 신청 수량
) {

}
