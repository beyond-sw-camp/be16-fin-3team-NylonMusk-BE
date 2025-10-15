package com.beyond.MKX.domain.ipo.allocation.service;

import com.beyond.MKX.domain.account.brokerage.service.BrokerageDepositAccountService;
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
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IpoSettlementService {
    private final IpoOfferingRepository offeringRepository;
    private final IpoSubscriptionRepository subscriptionRepository;
    private final IpoAllocationRepository allocationRepository;

    private final ExchangeAccountService exchangeAccountService;
    private final CorporationAccountService corporationAccountService;
    private final BrokerageDepositAccountService brokerageDepositAccountService;

    @Value("${exchange.account-number}") // 거래소 시스템 계좌번호 (설정에 넣으세요)
    private String exchangeAccountNumber;

    @Transactional
    public UUID settlePaymentsBySubscription(UUID subscriptionId) {
        IpoSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("공모 청약이 존재하지 않습니다."));
        if (subscription.getStatus() != SubscriptionStatus.PAID) {
            throw new IllegalArgumentException("납입/환불은 PAID 상태만 가능합니다.");
        }
        if (exchangeAccountNumber == null || exchangeAccountNumber.isBlank()) {
            throw new IllegalArgumentException("거래소 계좌 미설정");
        }

        IpoOffering off = offeringRepository.findById(subscription.getIpoOffering().getId())
                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));
        IpoAllocation alloc = allocationRepository
                .findTopByIpoSubscription_IdOrderByRoundNoDesc(subscriptionId)
                .orElse(null);

        long price = nz(off.getOfferPrice()); // 확정 공모가
        long allocQty = alloc == null ? 0L : nz(alloc.getAllocatedQuantity()); // 배정 수량
        long finalAmt = Math.multiplyExact(allocQty, price); // 최종 청구액 (배정 수량 * 확정 공모가)

        long appliedQty = nz(subscription.getAppliedQuantity());
        long depositAmt = calcDeposit(appliedQty, price, off.getDepositRate()); // 증거금(원단위 내림)

        long additional = Math.max(0, finalAmt - depositAmt);
        long refund    = Math.max(0, depositAmt - finalAmt);

//        증권사 예치 계좌 확보 (투자자 -> 청약/예치 => 증권사 => 거래소)
        var brokerageDeposit = brokerageDepositAccountService.getRequiredByBrokerageId(subscription.getBrokerageId());
        String brokerageDepositNo = brokerageDeposit.getAccountNumber();

        // CORPORATION만 우선 처리 (MEMBER는 TODO)
        if (subscription.getInvestorType() == InvestorType.CORPORATION) {
            if (additional > 0) {
                // 추가납입: Corporation → BrokerageDeposit → Exchange
                corporationAccountService.withdraw(subscription.getAccountId(), toBI(additional));
                brokerageDepositAccountService.deposit(brokerageDepositNo, toBI(additional));

                brokerageDepositAccountService.withdraw(brokerageDepositNo, toBI(additional));
                exchangeAccountService.deposit(exchangeAccountNumber, toBI(additional));
            } else if (refund > 0) {
                // 환불: Exchange → BrokerageDeposit → Corporation
                exchangeAccountService.withdraw(exchangeAccountNumber, toBI(refund));
                corporationAccountService.deposit(subscription.getAccountId(), toBI(refund));

                brokerageDepositAccountService.withdraw(brokerageDepositNo, toBI(refund));
                corporationAccountService.deposit(subscription.getAccountId(), toBI(refund));
            }
        } else {
            // TODO: MEMBER 계좌 정산 연동
            throw new UnsupportedOperationException("Member 투자자 정산(추가납입환불)은 추후 구현 대상입니다.");
        }

        return subscriptionId;
    }

    private long nz(Long v) { return v == null ? 0L : v; }
    private java.math.BigInteger toBI(long v) { return BigInteger.valueOf(v); }
    private long calcDeposit(long qty, long price, BigDecimal rate) {
        if (qty <= 0 || price <= 0 || rate == null) return 0L;
        return BigDecimal.valueOf(qty)
                .multiply(java.math.BigDecimal.valueOf(price))
                .multiply(rate)
                .setScale(0, RoundingMode.DOWN)
                .longValue();
    }
}
