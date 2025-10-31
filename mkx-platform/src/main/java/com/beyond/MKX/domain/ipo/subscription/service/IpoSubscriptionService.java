package com.beyond.MKX.domain.ipo.subscription.service;

import com.beyond.MKX.common.dto.AmountRequest;
import com.beyond.MKX.domain.account.brokerage.service.BrokerageDepositAccountService;
import com.beyond.MKX.domain.account.corporation.service.CorporationAccountService;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.ipo.offering.service.MemberAccountFeign;
import com.beyond.MKX.domain.ipo.subscription.dto.IpoSubscriptionReqDTO;
import com.beyond.MKX.domain.ipo.subscription.dto.IpoSubscriptionResDTO;
import com.beyond.MKX.domain.ipo.subscription.entity.InvestorType;
import com.beyond.MKX.domain.ipo.subscription.entity.IpoSubscription;
import com.beyond.MKX.domain.ipo.subscription.entity.SubscriptionStatus;
import com.beyond.MKX.domain.ipo.subscription.repository.IpoSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IpoSubscriptionService {
    private final IpoOfferingRepository offeringRepository;
    private final IpoSubscriptionRepository subscriptionRepository;
    private final CorporationRepository corporationRepository;
    private final MemberAccountFeign memberAccountFeign;
    private final Clock clock = Clock.systemDefaultZone();

    private final BrokerageDepositAccountService brokerageDepositAccountService;
    private final CorporationAccountService corporationAccountService;

    /**
     * 최소 제약:
     * - 공모 상태: OPEN
     * - 기간: now ∈ [subscriptionStart, subscriptionEnd]
     * - 중복 청약 방지: (offering, investorType, subscriberId) 1회
     * - 수량: lotSize 배수(가능하면), 1 이상
     * - 스냅샷: offerPrice/depositRate
     * - requiredDeposit 계산
     */

    @Transactional
    public IpoSubscriptionResDTO apply(IpoSubscriptionReqDTO subReqDto) {
//        1) 공모 조회
        IpoOffering ipoOffering = offeringRepository.findById(subReqDto.ipoOfferingId())
                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));

        if (subReqDto.investorType() == InvestorType.CORPORATION
                && ipoOffering.getIpo().getCorporation().getId().equals(subReqDto.subscriberId())) { // 청약을 한 자기 기업 청약 X
            throw new IllegalArgumentException("발행 기업은 자기 공모에 청약할 수 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now(clock);

//        2) 상태/기간 제약
        if (ipoOffering.getIpoOfferingStatus() != IpoOfferingStatus.OPEN) {
            throw new IllegalArgumentException("OPEN 상태에서만 청약이 가능합니다.");
        }
        if (ipoOffering.getSubscriptionStart() != null && now.isBefore(ipoOffering.getSubscriptionStart())) {
            throw new IllegalArgumentException("아직 청약 시작 전입니다.");
        }
        if (ipoOffering.getSubscriptionEnd() != null && now.isAfter(ipoOffering.getSubscriptionEnd())) {
            throw new IllegalArgumentException("청약이 마감되었습니다.");
        }

//        3) 중복 청약 방지 (한 계좌당 한 회차의 공모 청약 가능)
        boolean subscriptionExist = false;

        if (subReqDto.investorType() == InvestorType.CORPORATION) {
            // 기업 투자자: accountId로 중복 체크
            subscriptionExist = subscriptionRepository.existsByIpoOffering_IdAndAccountId(
                    subReqDto.ipoOfferingId(), subReqDto.accountId()
            );
        } else if (subReqDto.investorType() == InvestorType.INDIVIDUAL) {
            // 개인 투자자: accountNumber로 중복 체크
            subscriptionExist = subscriptionRepository.existsByIpoOffering_IdAndAccountNumber(
                    subReqDto.ipoOfferingId(), subReqDto.accountNumber()
            );
        }

        if (subscriptionExist) {
            throw new IllegalArgumentException("동일 계좌로 이미 청약하셨습니다.");
        }

//        4) 수량 제약 (필수 : 최소 1개 이상, lotSize의 배수만큼 청약 신청 가능)
        if (subReqDto.appliedQuantity() <= 0) {
            throw new IllegalArgumentException("신청 수량은 1 이상이어야 합니다.");
        }
        if (subReqDto.appliedQuantity() > ipoOffering.getOfferQuantity()) {
            throw new IllegalStateException("신청 수량이 공모 물량을 초과하였습니다.");
        }

        if (ipoOffering.getLotSize() != null && ipoOffering.getLotSize() > 0) {
            if (subReqDto.appliedQuantity() % ipoOffering.getLotSize() != 0) {
                throw new IllegalArgumentException("청약 수량은 청약 단위(lotSize)의 배수만큼이어야 합니다.");
            }
        }

//        5) 스냅샷 값 결정
        long priceSnapshot = resolveEffectivePrice(ipoOffering);             // 확정가가 있으면 사용, 없으면 희망 공모가 최댓값 사용
        BigDecimal depositRateSnapshot = ipoOffering.getDepositRate();       // DECIMAL(5,2)

//      applidedQty(신청 수량) * price * rate/100
        BigDecimal appliedAmount = BigDecimal.valueOf(subReqDto.appliedQuantity())
                .multiply(BigDecimal.valueOf(priceSnapshot));
        if (depositRateSnapshot == null) {
            throw new IllegalArgumentException("증거금률 미설정");
        }
        long requiredDeposit = appliedAmount
                .multiply(depositRateSnapshot)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .longValueExact();

        // 6) [자금 이동] 투자자 → 증권사 예치 (기업 투자자만 우선)
        if (subReqDto.investorType() == InvestorType.CORPORATION) {
            var brokerageDeposit = brokerageDepositAccountService.getRequiredByBrokerageId(subReqDto.brokerageId());
            String brokerageDepositNo = brokerageDeposit.getAccountNumber();

            // 출금(기업 계좌 UUID 기준) → 입금(증권사 예치 계좌번호 기준)
            corporationAccountService.withdraw(subReqDto.accountId(), BigInteger.valueOf(requiredDeposit));
            brokerageDepositAccountService.deposit(brokerageDepositNo, BigInteger.valueOf(requiredDeposit));
        } else {
            // TODO: MEMBER(개인) 계좌 흐름 연동
            var brokerageDeposit = brokerageDepositAccountService.getRequiredByBrokerageId(subReqDto.brokerageId());
            String brokerageDepositNo = brokerageDeposit.getAccountNumber();
            memberAccountFeign.withdraw(subReqDto.accountNumber(), new AmountRequest(BigInteger.valueOf(requiredDeposit)));
            brokerageDepositAccountService.deposit(brokerageDepositNo, BigInteger.valueOf(requiredDeposit));
        }

        // 7) 저장
        IpoSubscription ipoSubscription = IpoSubscription.builder()
                .ipoOffering(ipoOffering)
                .investorType(subReqDto.investorType())
                .subscriberId(subReqDto.subscriberId())
                .brokerageId(subReqDto.brokerageId())
                .accountId(subReqDto.accountId())
                .accountNumber(subReqDto.accountNumber())  // 👈 개인 청약 시 계좌번호 저장
                .appliedQuantity(subReqDto.appliedQuantity())
                .offerPriceSnapshot(priceSnapshot)
                .depositRateSnapshot(depositRateSnapshot)
                .requiredDeposit(requiredDeposit)
                .refundedAmount(0L)
                .status(SubscriptionStatus.PAID)
                .appliedAt(now)
                .paidAt(now)
                .build();

        subscriptionRepository.save(ipoSubscription);

        subscriptionRepository.flush(); // 방금 저장분이 합계 쿼리에 반영되도록 보장
        IpoSubscriptionResDTO dto = IpoSubscriptionResDTO.from(ipoSubscription);
        dto.setCompetitionRatioX(computeCompetitionRatioX(ipoOffering));
        return dto; // ✅ 계산된 경쟁RatioX 포함하여 반환

    }

    // 확정가가 있으면 그 값을, 없으면 밴드 상단을 사용(보수적 증거금).
    private long resolveEffectivePrice(IpoOffering ipoOffering) {
        Long price = ipoOffering.getOfferPrice();
        if (price != null && price > 0) {
            return price;
        }
        throw new IllegalStateException("확정 공모가가 설정되지 않았습니다.");
    }

    @Transactional
    public IpoSubscriptionResDTO cancel(UUID subscriptionId) {
        IpoSubscription ipoSubscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("청약을 찾을 수 없습니다."));

        IpoOffering o = ipoSubscription.getIpoOffering();
        LocalDateTime now = LocalDateTime.now(clock);

        // 1) 기간/상태 가드: OPEN & now < subscriptionEnd
        if (o.getIpoOfferingStatus() != IpoOfferingStatus.OPEN) {
            throw new IllegalStateException("OPEN 상태에서만 취소할 수 있습니다.");
        }
        if (o.getSubscriptionEnd() != null && !now.isBefore(o.getSubscriptionEnd())) {
            throw new IllegalStateException("청약 마감 이후에는 취소할 수 없습니다.");
        }

        // 2) 취소 가능한 현재 상태: PAID 또는 APPLIED
        if (ipoSubscription.getStatus() != SubscriptionStatus.PAID && ipoSubscription.getStatus() != SubscriptionStatus.APPLIED) {
            throw new IllegalStateException("해당 상태에서는 취소할 수 없습니다.");
        }
        if (ipoSubscription.getStatus() == SubscriptionStatus.CANCELLED) {
            return IpoSubscriptionResDTO.from(ipoSubscription);
        }

        // 3) 환불 처리: requiredDeposit 전액 환불로 기록
        long req = ipoSubscription.getRequiredDeposit() == null ? 0L : ipoSubscription.getRequiredDeposit();
        long ref = ipoSubscription.getRefundedAmount() == null ? 0L : ipoSubscription.getRefundedAmount();
        long refundable = Math.max(req - ref, 0);

        // 4) [자금 이동] 증권사 예치 → 투자자
        if (ipoSubscription.getInvestorType() == InvestorType.CORPORATION) {
            var brokerageDeposit = brokerageDepositAccountService.getRequiredByBrokerageId(ipoSubscription.getBrokerageId());
            String brokerageDepositNo = brokerageDeposit.getAccountNumber();

            if (refundable > 0) {
                brokerageDepositAccountService.withdraw(brokerageDepositNo, BigInteger.valueOf(refundable));
                corporationAccountService.deposit(ipoSubscription.getAccountId(), BigInteger.valueOf(refundable));
            }
        } else {
            var brokerageDeposit = brokerageDepositAccountService.getRequiredByBrokerageId(ipoSubscription.getBrokerageId());
            String brokerageDepositNo = brokerageDeposit.getAccountNumber();
            // TODO: MEMBER(개인) 계좌 환불 흐름
            if (refundable > 0) {
                brokerageDepositAccountService.withdraw(brokerageDepositNo, BigInteger.valueOf(refundable));
                memberAccountFeign.deposit(ipoSubscription.getAccountNumber(), new AmountRequest(BigInteger.valueOf(refundable)));
            }
        }

//      5) 상태/환불 기록  long refundable = (ipoSubscription.getRequiredDeposit() == null ? 0L : ipoSubscription.getRequiredDeposit());
        ipoSubscription.setStatus(SubscriptionStatus.CANCELLED);
        ipoSubscription.setCancelledAt(now);
        ipoSubscription.setRefundedAmount(ref + refundable);

        // ✅ DB 변경이 합계 쿼리에 반영되도록 보장
        subscriptionRepository.flush();

        // ✅ 경쟁률 계산 주입
        IpoSubscriptionResDTO dto = IpoSubscriptionResDTO.from(ipoSubscription);
        dto.setCompetitionRatioX(computeCompetitionRatioX(o));
        return dto;
    }

    @Transactional
    public IpoSubscriptionResDTO get(UUID subscriptionId) {
        IpoSubscription ipoSubscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("청약을 찾을 수 없습니다."));

        IpoSubscriptionResDTO dto = IpoSubscriptionResDTO.from(ipoSubscription);
        dto.setCompetitionRatioX(computeCompetitionRatioX(ipoSubscription.getIpoOffering())); // ✅ 최신 경쟁률
        return dto;
    }

    //    경쟁률 계산기
    private BigDecimal computeCompetitionRatioX(IpoOffering o) {
        long paidQty = subscriptionRepository
                .sumAppliedQuantityByOffering(o.getId(), SubscriptionStatus.PAID);
        long offerQty = (o.getOfferQuantity() == null ? 0L : o.getOfferQuantity());
        if (offerQty <= 0) return null;
        return BigDecimal.valueOf(paidQty)
                .divide(BigDecimal.valueOf(offerQty), 2, RoundingMode.HALF_UP); // 1.23 형태
    }

    @Transactional(readOnly = true)
    public List<IpoSubscriptionResDTO> findAll(UUID offeringId) {
        List<IpoSubscription> subscriptions =
                subscriptionRepository.findAllByOfferingIdAndStatus(offeringId, SubscriptionStatus.PAID);

        return subscriptions.stream()
                .map(sub -> {
                    IpoSubscriptionResDTO dto = IpoSubscriptionResDTO.from(sub);
                    dto.setSubscriberName(resolveSubscriberNameForCorporationView(sub));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 발행사 관점: 기관만 실명, 개인은 익명 처리
     */
    private String resolveSubscriberNameForCorporationView(IpoSubscription sub) {
        if (sub.getInvestorType() == InvestorType.CORPORATION) {
            return corporationRepository.findById(sub.getSubscriberId())
                    .map(c -> c.getNameKo())
                    .orElse("알 수 없음");
        }
        // 개인투자자는 이름 노출 금지
        return "개인 투자자";
    }

    //    개인 투자자 청약 조회
    @Transactional(readOnly = true)
    public List<IpoSubscriptionResDTO> findAllByMember(UUID subscriberId, InvestorType type) {
        List<IpoSubscription> list = subscriptionRepository.findAllBySubscriberIdAndInvestorType(subscriberId, type);

        return list.stream()
                .map(sub -> {
                    IpoSubscriptionResDTO dto = IpoSubscriptionResDTO.from(sub);
                    dto.setCompetitionRatioX(computeCompetitionRatioX(sub.getIpoOffering()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    //    기업 투자자 청약 조회
    @Transactional(readOnly = true)
    public List<IpoSubscriptionResDTO> findAllByCorporation(UUID subscriberId, InvestorType type) {
        List<IpoSubscription> list = subscriptionRepository.findAllBySubscriberIdAndInvestorType(subscriberId, type);

        return list.stream()
                .map(sub -> {
                    IpoSubscriptionResDTO dto = IpoSubscriptionResDTO.from(sub);
                    dto.setCompetitionRatioX(computeCompetitionRatioX(sub.getIpoOffering()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /** 향후 추가 공모 로직 시간이 된다면 쓸 예정 ... */
//    @Transactional
//    public IpoSubscriptionResDTO deposit(UUID subscriptionId, long depositAmount) {
//        if (depositAmount <= 0) throw new IllegalArgumentException("납입 금액은 0보다 커야 합니다.");
//
//        IpoSubscription ipoSubscription = subscriptionRepository.findById(subscriptionId)
//                .orElseThrow(() -> new IllegalArgumentException("청약을 찾을 수 없습니다."));
//
//        // 상태 체크: APPLIED → PAID 만 허용
//        if (ipoSubscription.getStatus() != SubscriptionStatus.APPLIED) {
//            throw new IllegalStateException("APPLIED 상태에서만 납입 처리 가능합니다.");
//        }
//
//        // 계좌 존재(최소 제약)
//        if (ipoSubscription.getAccountId() == null) {
//            throw new IllegalStateException("납입 계좌가 지정되지 않았습니다.");
//        }
//
//        // 스냅샷된 '필요 증거금' 사용 (apply 시점에 계산/저장됨)
//        long required = ipoSubscription.getRequiredDeposit();
//        if (depositAmount < required) {
//            throw new IllegalArgumentException("납입 금액(" + depositAmount + "원)은 필요 증거금(" + required + "원)보다 작습니다.");
//        }
//
//        // 더티체킹으로 업데이트
//        ipoSubscription.setDepositAmount(
//                (ipoSubscription.getDepositAmount() == null ? 0L : ipoSubscription.getDepositAmount()) + depositAmount
//        );
//        ipoSubscription.setStatus(SubscriptionStatus.PAID);
//        ipoSubscription.setPaidAt(LocalDateTime.now(clock));
//        return IpoSubscriptionResDTO.from(ipoSubscription);
//    }


}
