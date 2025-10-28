package com.beyond.MKX.domain.ipo.allocation.service;

import com.beyond.MKX.common.dto.AmountRequest;
import com.beyond.MKX.domain.account.brokerage.entity.BrokerageDepositAccount;
import com.beyond.MKX.domain.account.brokerage.service.BrokerageDepositAccountService;
import com.beyond.MKX.domain.account.corporation.service.CorporationAccountService;
import com.beyond.MKX.domain.account.exchange.service.ExchangeAccountService;
import com.beyond.MKX.domain.ipo.allocation.dto.IpoPayoutResDTO;
import com.beyond.MKX.domain.ipo.allocation.dto.IpoSettlementResDTO;
import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import com.beyond.MKX.domain.ipo.allocation.repository.IpoAllocationRepository;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.ipo.offering.service.MemberAccountFeign;
import com.beyond.MKX.domain.ipo.subscription.entity.InvestorType;
import com.beyond.MKX.domain.ipo.subscription.entity.IpoSubscription;
import com.beyond.MKX.domain.ipo.subscription.entity.SubscriptionStatus;
import com.beyond.MKX.domain.ipo.subscription.repository.IpoSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IpoSettlementService {
    private final IpoOfferingRepository offeringRepository;
    private final IpoSubscriptionRepository subscriptionRepository;
    private final IpoAllocationRepository allocationRepository;
    private final MemberAccountFeign memberAccountFeign;

    private final ExchangeAccountService exchangeAccountService;
    private final CorporationAccountService corporationAccountService;
    private final BrokerageDepositAccountService brokerageDepositAccountService;

    @Value("${exchange.account-number:900-0000-00000001}") // 거래소 시스템 계좌번호 (설정에 넣으세요)
    private String exchangeAccountNumber;

    private long nz(Long v) { return v == null ? 0L : v; }
    private BigInteger bi(long v) { return BigInteger.valueOf(v); }

    // ================================================================
    // ① 개별 청약 정산
    // ================================================================
    @Transactional
    public IpoSettlementResDTO settlePaymentsBySubscription(UUID subscriptionId) {
        IpoSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("공모 청약이 존재하지 않습니다."));

        IpoOffering offering = subscription.getIpoOffering();
        IpoAllocation allocation = allocationRepository
                .findTopByIpoSubscription_IdOrderByRoundNoDesc(subscriptionId)
                .orElse(null);

        BigInteger offerPrice = bi(nz(offering.getOfferPrice()));
        BigInteger allocatedQty = bi(allocation == null ? 0L : allocation.getAllocatedQuantity());
        BigInteger finalAmt = allocatedQty.multiply(offerPrice);
        BigInteger depositAmt = bi(nz(subscription.getRequiredDeposit()));

        BigInteger additional = BigInteger.ZERO;
        BigInteger refund = BigInteger.ZERO;
        int compare = finalAmt.compareTo(depositAmt);
        if (compare > 0) additional = finalAmt.subtract(depositAmt);
        else if (compare < 0) refund = depositAmt.subtract(finalAmt);

        BrokerageDepositAccount brokerageDeposit = brokerageDepositAccountService
                .getRequiredByBrokerageId(subscription.getBrokerageId());
        String brokerageDepositNo = brokerageDeposit.getAccountNumber();
        String exchangeAccountNo = exchangeAccountNumber;

        // === 1단계: 선이관 ===
        if (depositAmt.signum() > 0) {
            log.info("[STEP1] 예치금 선이관: 증권사({}) → 거래소({}) 금액={}", brokerageDepositNo, exchangeAccountNo, depositAmt);
            brokerageDepositAccountService.withdraw(brokerageDepositNo, depositAmt);
            exchangeAccountService.deposit(exchangeAccountNo, depositAmt);
        }

        // === 2단계: 추가납입 ===
        if (additional.signum() > 0) {
            log.info("[STEP2] 추가납입 발생: 투자자유형={}, 금액={}", subscription.getInvestorType(), additional);

            if (subscription.getInvestorType() == InvestorType.CORPORATION) {
                UUID corpAccountId = subscription.getAccountId();
                corporationAccountService.withdraw(corpAccountId, additional);
                brokerageDepositAccountService.deposit(brokerageDepositNo, additional);
                brokerageDepositAccountService.withdraw(brokerageDepositNo, additional);
                exchangeAccountService.deposit(exchangeAccountNo, additional);
            }

            else if (subscription.getInvestorType() == InvestorType.INDIVIDUAL) {
                String memberAccNo = subscription.getAccountNumber();
                memberAccountFeign.withdraw(memberAccNo, new AmountRequest(additional));
                brokerageDepositAccountService.deposit(brokerageDepositNo, additional);
                brokerageDepositAccountService.withdraw(brokerageDepositNo, additional);
                exchangeAccountService.deposit(exchangeAccountNo, additional);
            }
        }

        // === 3단계: 환불 ===
        else if (refund.signum() > 0) {
            log.info("[STEP3] 환불 발생: 투자자유형={}, 금액={}", subscription.getInvestorType(), refund);

            if (subscription.getInvestorType() == InvestorType.CORPORATION) {
                UUID corpAccountId = subscription.getAccountId();
                exchangeAccountService.withdraw(exchangeAccountNo, refund);
                brokerageDepositAccountService.deposit(brokerageDepositNo, refund);
                brokerageDepositAccountService.withdraw(brokerageDepositNo, refund);
                corporationAccountService.deposit(corpAccountId, refund);
            }

            else if (subscription.getInvestorType() == InvestorType.INDIVIDUAL) {
                String memberAccNo = subscription.getAccountNumber();
                exchangeAccountService.withdraw(exchangeAccountNo, refund);
                brokerageDepositAccountService.deposit(brokerageDepositNo, refund);
                brokerageDepositAccountService.withdraw(brokerageDepositNo, refund);
                memberAccountFeign.deposit(memberAccNo, new AmountRequest(refund));
            }
        }

        subscription.setStatus(SubscriptionStatus.SETTLED);
        subscriptionRepository.save(subscription);

        log.info("[SETTLEMENT COMPLETE] 청약 {} 정산 완료 — finalAmt={}, depositAmt={}, additional={}, refund={}",
                subscriptionId, finalAmt, depositAmt, additional, refund);

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

    // ================================================================
    // ② Batch 정산 (공모 전체)
    // ================================================================
    @Transactional
    public List<IpoSettlementResDTO> settleAllPaymentsByOffering(UUID offeringId) {
        log.info("[BATCH-SETTLE] 공모 {} 정산 시작", offeringId);
        IpoOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));

        List<IpoSubscription> subs = subscriptionRepository
                .findAllByOfferingIdAndStatus(offeringId, SubscriptionStatus.ALLOCATED);
        if (subs.isEmpty()) throw new IllegalStateException("배정(ALLOCATED) 상태 청약 없음");

        List<IpoSettlementResDTO> results = new ArrayList<>();
        for (IpoSubscription s : subs)
            results.add(settlePaymentsBySubscription(s.getId()));

        log.info("[BATCH-SETTLE COMPLETE] 공모 {} 전체 정산 완료 (총 {}건)", offeringId, subs.size());
        return results;
    }

    // ================================================================
    // ③ 발행사 송금
    // ================================================================
    @Transactional
    public IpoPayoutResDTO payoutOfferingToIssuer(UUID offeringId) {
        IpoOffering offering = offeringRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모 없음"));

        BigInteger totalProceeds = BigInteger
                .valueOf(Optional.ofNullable(offering.getAllocatedQuantity()).orElse(0L))
                .multiply(BigInteger.valueOf(offering.getOfferPrice()));
        BigInteger balance = exchangeAccountService.getByAccountNumber(exchangeAccountNumber).getBalance();

        log.info("[PAYOUT] 발행사 송금 준비 — 공모={}, 필요금액={}, 거래소잔액={}", offeringId, totalProceeds, balance);

        if (balance.compareTo(totalProceeds) < 0)
            throw new IllegalStateException("거래소 계좌 잔액 부족 — 모든 정산 완료 전 송금 불가");

        var issuerAcc = corporationAccountService.getByCorporationId(
                offering.getIpo().getCorporation().getId());

        exchangeAccountService.withdraw(exchangeAccountNumber, totalProceeds);
        log.info(" └ 거래소 계좌 출금 완료 후 공모발행기업 예치 입금 완료");
        corporationAccountService.deposit(issuerAcc.getId(), totalProceeds);
        log.info(" └ 발행기업 계좌 입금 완료");

        offering.settle(Optional.ofNullable(offering.getAllocatedQuantity()).orElse(0L));
        offeringRepository.save(offering);

        log.info("[PAYOUT COMPLETE] 발행사 송금 완료 — 공모={}, 금액={}", offeringId, totalProceeds);
        return IpoPayoutResDTO.of(offering, totalProceeds);
    }
}
