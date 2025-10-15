package com.beyond.MKX.domain.ipo.allocation.service;

import com.beyond.MKX.domain.account.corporation.service.CorporationAccountService;
import com.beyond.MKX.domain.account.exchange.service.ExchangeAccountService;
import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import com.beyond.MKX.domain.ipo.allocation.repository.IpoAllocationRepository;
import com.beyond.MKX.domain.ipo.ipo.repository.IpoRepository;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.ipo.subscription.entity.InvestorType;
import com.beyond.MKX.domain.ipo.subscription.entity.IpoSubscription;
import com.beyond.MKX.domain.ipo.subscription.entity.SubscriptionStatus;
import com.beyond.MKX.domain.ipo.subscription.repository.IpoSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IpoSettlementService {
    private final IpoOfferingRepository offeringRepository;
    private final IpoSubscriptionRepository subscriptionRepository;
    private final IpoAllocationRepository allocationRepository;

    private final ExchangeAccountService exchangeAccountService;
    private final CorporationAccountService corporationAccountService;

    @Value("${exchange.account-number}") // 거래소 시스템 계좌번호 (설정에 넣으세요)
    private String exchangeAccountNumber;

    @Transactional
    public UUID settlePaymentsBySubscription(UUID subscriptionId) {
        IpoSubscription s = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("공모 청약이 존재하지 않습니다."));
        if (s.getStatus() != SubscriptionStatus.PAID) {
            throw new IllegalArgumentException("납입/환불은 PAID 상태만 가능합니다.");
        }

        IpoOffering off = offeringRepository.findById(s.getIpoOffering().getId())
                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));
        IpoAllocation alloc = allocationRepository
                .findTopByIpoSubscription_IdOrderByRoundNoDesc(subscriptionId)
                .orElse(null);

        long price = nz(off.getOfferPrice());
        long allocQty = alloc == null ? 0L : nz(alloc.getAllocatedQuantity());
        long finalAmt = Math.multiplyExact(allocQty, price); // 최종 청구액

        long appliedQty = nz(s.getAppliedQuantity());
        long depositAmt = calcDeposit(appliedQty, price, off.getDepositRate()); // 증거금(원단위 내림)

        long additional = Math.max(0, finalAmt - depositAmt);
        long refund    = Math.max(0, depositAmt - finalAmt);

        // CORPORATION만 우선 처리 (MEMBER는 TODO)
        if (s.getInvestorType() == InvestorType.CORPORATION) {
            if (additional > 0) {
                // 법인 → 거래소
                corporationAccountService.withdraw(s.getAccountId(), toBI(additional));
                exchangeAccountService.deposit(exchangeAccountNumber, toBI(additional));
            } else if (refund > 0) {
                // 거래소 → 법인
                exchangeAccountService.withdraw(exchangeAccountNumber, toBI(refund));
                corporationAccountService.deposit(s.getAccountId(), toBI(refund));
            }
        } else {
            // TODO: MEMBER 계좌 정산 연동
        }

        return subscriptionId;
    }

    private long nz(Long v) { return v == null ? 0L : v; }
    private java.math.BigInteger toBI(long v) { return BigInteger.valueOf(v); }
    private long calcDeposit(long qty, long price, BigDecimal rate) {
        if (qty <= 0 || price <= 0 || rate == null) return 0L;
        return java.math.BigDecimal.valueOf(qty)
                .multiply(java.math.BigDecimal.valueOf(price))
                .multiply(rate)
                .setScale(0, java.math.RoundingMode.DOWN)
                .longValue();
    }
}
