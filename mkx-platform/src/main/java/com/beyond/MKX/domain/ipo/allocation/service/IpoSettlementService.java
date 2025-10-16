package com.beyond.MKX.domain.ipo.allocation.service;

import com.beyond.MKX.domain.account.brokerage.service.BrokerageDepositAccountService;
import com.beyond.MKX.domain.account.corporation.service.CorporationAccountService;
import com.beyond.MKX.domain.account.exchange.service.ExchangeAccountService;
import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import com.beyond.MKX.domain.ipo.allocation.repository.IpoAllocationRepository;
import com.beyond.MKX.domain.ipo.ipo.repository.IpoRepository;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
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

    @Value("${exchange.account-number:900-0000-00000001}") // 거래소 시스템 계좌번호 (설정에 넣으세요)
    private String exchangeAccountNumber;

    @Transactional
    public UUID settlePaymentsBySubscription(UUID subscriptionId) {
        IpoSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("공모 청약이 존재하지 않습니다."));
//        if (subscription.getStatus() != SubscriptionStatus.ALLOCATED) {
//            throw new IllegalArgumentException("납입/환불은 배정(ALLOCATED) 상태만 가능합니다.");
//        }
        if (exchangeAccountNumber == null || exchangeAccountNumber.isBlank()) {
            throw new IllegalArgumentException("거래소 계좌 미설정");
        }

        IpoOffering offering = offeringRepository.findById(subscription.getIpoOffering().getId())
                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));

//        if (offering.getIpoOfferingStatus() != IpoOfferingStatus.PRICE_FIXED
//        || offering.getIpoOfferingStatus() != IpoOfferingStatus.ALLOCATED) {
//            throw new IllegalArgumentException("공모가 확정 상태 또는 배정 상태에서만 정산할 수 있습니다.");
//        }

        IpoAllocation allocation = allocationRepository
                .findTopByIpoSubscription_IdOrderByRoundNoDesc(subscriptionId)
                .orElse(null);

        BigInteger price = bi(nz(offering.getOfferPrice())); // 확정 공모가
        BigInteger allocQty = bi(allocation == null ? 0L : nz(allocation.getAllocatedQuantity())); // 배정 수량
        BigInteger finalAmt = allocQty.multiply(price); // 최종 청구액 (배정 수량 * 확정 공모가)

        BigInteger depositAmt = BigInteger.valueOf(nz(subscription.getRequiredDeposit())); // 증거금(원단위 내림), 재계산 X 스냅샷 사용!

        int cmp = finalAmt.compareTo(depositAmt);
        BigInteger additional = cmp > 0 ? finalAmt.subtract(depositAmt) : BigInteger.ZERO;
        BigInteger refund = cmp < 0 ? depositAmt.subtract(finalAmt) : BigInteger.ZERO;

//        증권사 예치 계좌 확보 (투자자 -> 청약/예치 => 증권사 => 거래소)
        var brokerageDeposit = brokerageDepositAccountService.getRequiredByBrokerageId(subscription.getBrokerageId());
        String brokerageDepositNo = brokerageDeposit.getAccountNumber();

        // CORPORATION만 우선 처리 (MEMBER는 TODO)
        if (subscription.getInvestorType() == InvestorType.CORPORATION) {
            UUID corporationAccountId = subscription.getAccountId();

            if (additional.signum() > 0) {
                // 추가납입: Corporation → BrokerageDeposit → Exchange
                corporationAccountService.withdraw(corporationAccountId, additional);
                brokerageDepositAccountService.deposit(brokerageDepositNo, additional);

                brokerageDepositAccountService.withdraw(brokerageDepositNo, additional);
                exchangeAccountService.deposit(exchangeAccountNumber, additional);
            } else if (refund.signum() > 0) {
                // 환불: Exchange → BrokerageDeposit → Corporation
                exchangeAccountService.withdraw(exchangeAccountNumber, refund);
                brokerageDepositAccountService.deposit(brokerageDepositNo, refund);

                brokerageDepositAccountService.withdraw(brokerageDepositNo, refund);
                corporationAccountService.deposit(subscription.getAccountId(), refund);
            }
        } else {
            // TODO: MEMBER 계좌 정산 연동
            throw new UnsupportedOperationException("Member 투자자 정산(추가납입환불)은 추후 구현 대상입니다.");
        }

        return subscriptionId;
    }

    private long nz(Long v) { return v == null ? 0L : v; }
    private BigInteger bi(long v) { return BigInteger.valueOf(v); }
    private java.math.BigInteger toBI(long v) { return BigInteger.valueOf(v); }
    private BigInteger calcDeposit(BigInteger qty, BigInteger price, BigDecimal rate) {
        BigDecimal decimalRate = rate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        if (qty.signum() <= 0 || price.signum() <= 0 || rate == null) return BigInteger.ZERO;
        return new BigDecimal(qty)
                .multiply(new BigDecimal(price))
                .multiply(decimalRate)
                .setScale(0, RoundingMode.DOWN)
                .toBigInteger();
    }

    @Transactional
    public UUID payoutOfferingToIssuer(UUID offeringId) {
        IpoOffering offering = offeringRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모 없음"));

        if (offering.getIpoOfferingStatus() == IpoOfferingStatus.SETTLED) return offeringId;
        if (offering.getIpoOfferingStatus() != IpoOfferingStatus.ALLOCATED) {
            throw new IllegalStateException("ALLOCATED 상태에서만 송금");
        }

        // 1) sQty = 배정 스냅샷값
        long sQty = Optional.ofNullable(offering.getAllocatedQuantity()).orElse(0L);

        // (선택) 안전검증: 실제 Allocation 합과 일치하는지 체크만 하고, 소스는 스냅샷값을 신뢰
        var price = BigInteger.valueOf(offering.getOfferPrice());
        var allocations = allocationRepository.findAllByOfferingId(offeringId);
        long sumAllocQty = allocations.stream().mapToLong(IpoAllocation::getAllocatedQuantity).sum();
        if (sQty != sumAllocQty) {
            throw new IllegalStateException("배정 스냅샷과 실제 합계가 일치하지 않습니다.");
        }

        BigInteger totalProceeds = BigInteger.valueOf(sQty).multiply(price);
        BigInteger payout = totalProceeds; // 수수료 없으면 그대로

        var issuerAcc = corporationAccountService.getByCorporationId(
                offering.getIpo().getCorporation().getId()
        );
        exchangeAccountService.withdraw(exchangeAccountNumber, payout);
        corporationAccountService.deposit(issuerAcc.getId(), payout);

        // 정산 완료: issuedQuantity에 스냅샷값 기록
        offering.settle(sQty); // issuedQuantity = sQty, 상태 SETTLED
        offeringRepository.save(offering);
        return offeringId;
    }

}
