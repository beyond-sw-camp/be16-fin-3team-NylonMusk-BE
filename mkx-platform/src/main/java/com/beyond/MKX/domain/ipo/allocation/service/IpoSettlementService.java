package com.beyond.MKX.domain.ipo.allocation.service;

import com.beyond.MKX.common.dto.AmountRequest;
import com.beyond.MKX.domain.account.brokerage.entity.BrokerageDepositAccount;
import com.beyond.MKX.domain.account.brokerage.service.BrokerageDepositAccountService;
import com.beyond.MKX.domain.account.corporation.service.CorporationAccountService;
import com.beyond.MKX.domain.account.exchange.service.ExchangeAccountService;
import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.entity.IpoAllocationOutbox;
import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.entity.OutboxStatus;
import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.repository.IpoAllocationOutboxRepository;
import com.beyond.MKX.domain.ipo.allocation.dto.IpoPayoutResDTO;
import com.beyond.MKX.domain.ipo.allocation.dto.IpoSettlementResDTO;
import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import com.beyond.MKX.domain.ipo.allocation.repository.IpoAllocationRepository;
import com.beyond.MKX.domain.ipo.ipo.dto.StockUpdateDTO;
import com.beyond.MKX.domain.ipo.ipo.service.IpoAllocationFeign;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingType;
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
    private final IpoAllocationOutboxRepository outboxRepository;
    private final MemberAccountFeign memberAccountFeign;
    private final IpoAllocationFeign orderingFeign;

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
        offeringRepository.findById(offeringId)
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

        // ================================================================
        // N차 공모인 경우 주식 보유 업데이트 처리
        // ================================================================
        if (offering.getOfferingType() == IpoOfferingType.FOLLOW_ON
                || offering.getOfferingType() == IpoOfferingType.RIGHTS_ISSUE) {

            try {
                UUID ipoId = offering.getIpo().getId();
                String ticker = Optional.ofNullable(offering.getIpo().getStockTicker()).orElse(null);

                if (ticker == null || ticker.isBlank()) {
                    log.warn("[PAYOUT-STOCK-UPDATE] 상장된 종목 티커를 찾을 수 없어 주식 업데이트를 건너뜁니다. offeringId={}", offeringId);
                } else {
                    Long offerPrice = offering.getOfferPrice();
                    String reason = offering.getOfferingType() == IpoOfferingType.FOLLOW_ON
                            ? "FOLLOW_ON_ALLOCATION"
                            : "RIGHTS_ISSUE_ALLOCATION";

                    log.info("[PAYOUT-STOCK-UPDATE] N차 공모 주식 배정 시작. offeringId={}, ipoId={}, ticker={}, price={}",
                            offeringId, ipoId, ticker, offerPrice);

                    List<IpoAllocationOutbox> events = new ArrayList<>(
                            outboxRepository.findAllByIpoIdAndStatus(ipoId, OutboxStatus.PENDING)
                    );
                    events.removeIf(ev -> !offeringId.equals(ev.getOfferingId()));

                    log.info("[PAYOUT-STOCK-UPDATE] 조회된 PENDING 이벤트 수: {}", events.size());

                    if (!events.isEmpty()) {
                        int ok = 0, fail = 0;

                        for (var ev : events) {
                            log.info("[PAYOUT-STOCK-UPDATE] 이벤트 처리 시작. allocationId={}, accountId={}, qty={}",
                                    ev.getAllocationId(), ev.getAccountId(), ev.getQty());

                            var dto = StockUpdateDTO.builder()
                                    .idempotencyKey(ev.getAllocationId())
                                    .allocationId(ev.getAllocationId())
                                    .offeringId(ev.getOfferingId())
                                    .memberAccountId(ev.getAccountId())
                                    .brokerageId(ev.getBrokerageId())
                                    .ticker(ticker)
                                    .qtyDelta(ev.getQty())
                                    .unitPrice(offerPrice)
                                    .reason(reason)
                                    .build();

                            try {
                                log.info("[PAYOUT-STOCK-UPDATE] ordering-service 호출 시작");
                                orderingFeign.applyStockUpdate(dto);
                                ev.markSent();
                                ok++;
                                log.info("[PAYOUT-STOCK-UPDATE] ordering-service 호출 성공");
                            } catch (Exception sendEx) {
                                ev.markFailed();
                                fail++;
                                log.error("[PAYOUT-STOCK-UPDATE] ordering-service 호출 실패. allocationId={}, error={}",
                                        ev.getAllocationId(), sendEx.getMessage(), sendEx);
                            }
                        }

                        outboxRepository.saveAll(events);
                        log.info("[PAYOUT-STOCK-UPDATE] 주식 배정 완료. 성공={}, 실패={}", ok, fail);
                    } else {
                        log.warn("[PAYOUT-STOCK-UPDATE] PENDING 상태의 이벤트가 없습니다!");
                    }
                }
            } catch (Exception ex) {
                // 주식 업데이트 실패해도 발행사 송금은 완료되었으므로 로그만 남기고 계속 진행
                log.error("[PAYOUT-STOCK-UPDATE] N차 공모 주식 배정 중 예외 발생. offeringId={}", offeringId, ex);
            }
        }



        return IpoPayoutResDTO.of(offering, totalProceeds);
    }
}
