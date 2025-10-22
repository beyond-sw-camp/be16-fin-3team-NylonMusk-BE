package com.beyond.MKX.domain.ipo.allocation.service;

import com.beyond.MKX.domain.account.brokerage.entity.BrokerageDepositAccount;
import com.beyond.MKX.domain.account.brokerage.service.BrokerageDepositAccountService;
import com.beyond.MKX.domain.account.corporation.service.CorporationAccountService;
import com.beyond.MKX.domain.account.exchange.service.ExchangeAccountService;
import com.beyond.MKX.domain.ipo.allocation.dto.IpoPayoutResDTO;
import com.beyond.MKX.domain.ipo.allocation.dto.IpoSettlementResDTO;
import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import com.beyond.MKX.domain.ipo.allocation.repository.IpoAllocationRepository;
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


    // ================================================================
    // ① 개별 청약 정산 (기존 메서드 유지)
    // ================================================================
    @Transactional
    public IpoSettlementResDTO settlePaymentsBySubscription(UUID subscriptionId) {
        IpoSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("공모 청약이 존재하지 않습니다."));
//        if (subscription.getStatus() != SubscriptionStatus.ALLOCATED) {
//            throw new IllegalArgumentException("납입/환불은 배정(ALLOCATED) 상태만 가능합니다.");
//        } ---> 청약 존재여부만 따지면 되지

        IpoOffering offering = subscription.getIpoOffering();
        IpoAllocation allocation = allocationRepository
                .findTopByIpoSubscription_IdOrderByRoundNoDesc(subscriptionId)
                .orElse(null);

// === 기본 계산 ===
        BigInteger offerPrice = bi(nz(offering.getOfferPrice())); // 확정 공모가
        BigInteger allocatedQty = bi(allocation == null ? 0L : allocation.getAllocatedQuantity()); // 배정 수량
        BigInteger finalAmt = allocatedQty.multiply(offerPrice); // 실제 청구금액
        BigInteger depositAmt = bi(nz(subscription.getRequiredDeposit())); // 증거금 (스냅샷)

// === 추가납입 / 환불 계산 ===
        BigInteger additional = BigInteger.ZERO;
        BigInteger refund = BigInteger.ZERO;
        int compare = finalAmt.compareTo(depositAmt);
        if (compare > 0) {
            additional = finalAmt.subtract(depositAmt); // 추가납입 필요
        } else if (compare < 0) {
            refund = depositAmt.subtract(finalAmt); // 환불 필요
        }

// === 계좌 정보 ===
        BrokerageDepositAccount brokerageDeposit = brokerageDepositAccountService
                .getRequiredByBrokerageId(subscription.getBrokerageId());
        String brokerageDepositNo = brokerageDeposit.getAccountNumber();
        String exchangeAccountNo = exchangeAccountNumber;

// === 1단계: 선이관 (예치금 중 최소금액만 거래소로 이동) ===
        if (depositAmt.signum() > 0) {
            brokerageDepositAccountService.withdraw(brokerageDepositNo, depositAmt);
            exchangeAccountService.deposit(exchangeAccountNo, depositAmt);
        }

// === 2단계: 추가납입 ===
        //    기업 → 증권사예치 → 거래소 (자금 추적 가능)
        if (additional.signum() > 0 && subscription.getInvestorType() == InvestorType.CORPORATION) {
            UUID corpAccountId = subscription.getAccountId();
            corporationAccountService.withdraw(corpAccountId, additional);
            brokerageDepositAccountService.deposit(brokerageDepositNo, additional);
            brokerageDepositAccountService.withdraw(brokerageDepositNo, additional);
            exchangeAccountService.deposit(exchangeAccountNo, additional);
        }

        // 🔸 환불 : 환불은 거래소 → 증권사 → 투자기업으로 명확히 분리
        else if (refund.signum() > 0 && subscription.getInvestorType() == InvestorType.CORPORATION) {
            UUID corpAccountId = subscription.getAccountId();
            exchangeAccountService.withdraw(exchangeAccountNo, refund);
            brokerageDepositAccountService.deposit(brokerageDepositNo, refund);
            brokerageDepositAccountService.withdraw(brokerageDepositNo, refund);
            corporationAccountService.deposit(corpAccountId, refund);
        }
// === 4단계: TODO MEMBER 처리 ===
        else if (subscription.getInvestorType() == InvestorType.INDIVIDUAL) {
            throw new UnsupportedOperationException("INDIVIDUAL 투자자 정산(추가납입/환불)은 추후 구현 대상입니다.");
        }

        subscription.setStatus(SubscriptionStatus.SETTLED);
        subscriptionRepository.save(subscription);

// === 결과 DTO 반환 ===
        return IpoSettlementResDTO.of(
                subscription.getId().toString(),
                offering.getId().toString(),
                subscription.getInvestorType(),
                brokerageDepositNo,
                exchangeAccountNo,
                finalAmt,
                depositAmt,
                depositAmt,
                additional,
                refund
        );
    }

    private long nz(Long v) { return v == null ? 0L : v; }
    private BigInteger bi(long v) { return BigInteger.valueOf(v); }

    // ================================================================
    // ② Batch 정산 (공모 전체)
    // ================================================================
    @Transactional
    public List<IpoSettlementResDTO> settleAllPaymentsByOffering(UUID offeringId) {
        IpoOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));

        List<IpoSubscription> subs = subscriptionRepository
                .findAllByOfferingIdAndStatus(offeringId, SubscriptionStatus.ALLOCATED);
        if (subs.isEmpty()) throw new IllegalStateException("배정(ALLOCATED) 상태 청약 없음");

        List<IpoSettlementResDTO> results = new ArrayList<>();
        for (IpoSubscription s : subs)
            results.add(settlePaymentsBySubscription(s.getId()));
        return results;
    }

    // ================================================================
    // ③ 발행사 송금
    // ================================================================
    @Transactional
    public IpoPayoutResDTO payoutOfferingToIssuer(UUID offeringId) {
        IpoOffering offering = offeringRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모 없음"));

        // 🔸 핵심 수정: 송금 전 거래소 잔액 충분성 검증
        BigInteger totalProceeds = BigInteger
                .valueOf(Optional.ofNullable(offering.getAllocatedQuantity()).orElse(0L))
                .multiply(BigInteger.valueOf(offering.getOfferPrice()));
        BigInteger balance = exchangeAccountService.getByAccountNumber(exchangeAccountNumber).getBalance();
        if (balance.compareTo(totalProceeds) < 0)
            throw new IllegalStateException("거래소 계좌 잔액 부족 — 모든 정산 완료 전 송금 불가");

        // 🔸 핵심 수정: 거래소 → 증권사 → 발행기업 흐름 유지
        var issuerAcc = corporationAccountService.getByCorporationId(
                offering.getIpo().getCorporation().getId());
        // 거래소 → 증권사 예치금
        var brokerage = brokerageDepositAccountService.getRequiredByBrokerageId(
                issuerAcc.getCorporationId());
        brokerageDepositAccountService.deposit(brokerage.getAccountNumber(), totalProceeds);
        // 증권사 → 발행기업
        brokerageDepositAccountService.withdraw(brokerage.getAccountNumber(), totalProceeds);
        corporationAccountService.deposit(issuerAcc.getId(), totalProceeds);

        offering.settle(Optional.ofNullable(offering.getAllocatedQuantity()).orElse(0L));
        offeringRepository.save(offering);
        return IpoPayoutResDTO.of(offering, totalProceeds);
    }
}
