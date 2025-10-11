package com.beyond.MKX.domain.ipo.subscription.service;

import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.ipo.subscription.dto.IpoSubscriptionReqDTO;
import com.beyond.MKX.domain.ipo.subscription.dto.IpoSubscriptionResDTO;
import com.beyond.MKX.domain.ipo.subscription.entity.IpoSubscription;
import com.beyond.MKX.domain.ipo.subscription.entity.SubscriptionStatus;
import com.beyond.MKX.domain.ipo.subscription.repository.IpoSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IpoSubscriptionService {
    private final IpoOfferingRepository offeringRepository;
    private final IpoSubscriptionRepository subscriptionRepository;
    private final Clock clock = Clock.systemDefaultZone();

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
    public IpoSubscriptionResDTO apply(IpoSubscriptionReqDTO createReqDTO) {
//        1) 공모 조회
        IpoOffering ipoOffering = offeringRepository.findById(createReqDTO.ipoOfferingId())
                .orElseThrow(() -> new IllegalArgumentException("공모를 찾을 수 없습니다."));

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
        boolean subscriptionExist = subscriptionRepository.existsByIpoOffering_IdAndAccountId(
                createReqDTO.ipoOfferingId(), createReqDTO.accountId()
        );
        if (subscriptionExist) {
            throw new IllegalArgumentException("해당 계좌로는 이미 공모 청약 신청이 되었습니다.");
        }

//        4) 수량 제약 (필수 : 최소 1개 이상, lotSize의 배수만큼 청약 신청 가능)
        if (createReqDTO.appliedQuantity() <= 0) {
            throw new IllegalArgumentException("신청 수량은 1 이상이어야 합니다.");
        }
        if (ipoOffering.getLotSize() != null && ipoOffering.getLotSize() > 0) {
            if (createReqDTO.appliedQuantity() % ipoOffering.getLotSize() != 0) {
                throw new IllegalArgumentException("청약 수량은 청약 단위(lotSize)의 배수만큼이어야 합니다.");
            }
        }
//        5) 스냅샷 값 결정
        long priceSnapshot = resolveEffectivePrice(ipoOffering);             // 확정가가 있으면 사용, 없으면 희망 공모가 최댓값 사용
        BigDecimal depositRateSnapshot = ipoOffering.getDepositRate();       // DECIMAL(5,2)

//        6) 필요 증거금(보증금) 계산 : applidedQty(신청 수량) * price * rate/100
        BigDecimal appliedAmount = BigDecimal.valueOf(createReqDTO.appliedQuantity())
                .multiply(BigDecimal.valueOf(priceSnapshot));
        long requiredDeposit = appliedAmount
                .multiply(depositRateSnapshot)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .longValueExact();

        // 7) 저장
        IpoSubscription ipoSubscription = IpoSubscription.builder()
                .ipoOffering(ipoOffering)
                .investorType(createReqDTO.investorType())
                .subscriberId(createReqDTO.subscriberId())
                .brokerageId(createReqDTO.brokerageId())
                .accountId(createReqDTO.accountId())
                .appliedQuantity(createReqDTO.appliedQuantity())
                .offerPriceSnapshot(priceSnapshot)
                .depositRateSnapshot(depositRateSnapshot)
                .requiredDeposit(requiredDeposit)
                .paidAmount(0L)
                .refundedAmount(0L)
                .status(SubscriptionStatus.APPLIED)
                .appliedAt(now)
                .build();

        subscriptionRepository.save(ipoSubscription);

        return IpoSubscriptionResDTO.from(ipoSubscription);

    }
    // 확정가가 있으면 그 값을, 없으면 밴드 상단을 사용(보수적 증거금).
    private long resolveEffectivePrice(IpoOffering ipoOffering) {
        if (ipoOffering.getOfferPrice() != null && ipoOffering.getOfferPrice() > 0) return ipoOffering.getOfferPrice();
//        확정공모가가 없다면, 희망공모가 MAX로 함. 이후 공모가가 확정되면 환불처리
        if (ipoOffering.getPriceBandMax() != null && ipoOffering.getPriceBandMax() > 0) return ipoOffering.getPriceBandMax();
        throw new IllegalStateException("청약 단가를 결정할 수 없습니다(확정가/밴드 미설정).");
    }

    @Transactional
    public IpoSubscriptionResDTO deposit(UUID subscriptionId, long depositAmount) {
        if (depositAmount <= 0) throw new IllegalArgumentException("납입 금액은 0보다 커야 합니다.");

        IpoSubscription ipoSubscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("청약을 찾을 수 없습니다."));

        // 상태 체크: APPLIED → PAID 만 허용
        if (ipoSubscription.getStatus() != SubscriptionStatus.APPLIED) {
            throw new IllegalStateException("APPLIED 상태에서만 납입 처리 가능합니다.");
        }

        // 계좌 존재(최소 제약)
        if (ipoSubscription.getAccountId() == null) {
            throw new IllegalStateException("납입 계좌가 지정되지 않았습니다.");
        }

        // 스냅샷된 '필요 증거금' 사용 (apply 시점에 계산/저장됨)
        long required = ipoSubscription.getRequiredDeposit();
        if (depositAmount < required) {
            throw new IllegalArgumentException("납입 금액(" + depositAmount + "원)은 필요 증거금(" + required + "원)보다 작습니다.");
        }

        // 더티체킹으로 업데이트
        ipoSubscription.setPaidAmount(
                (ipoSubscription.getPaidAmount() == null ? 0L : ipoSubscription.getPaidAmount()) + depositAmount
        );
        ipoSubscription.setStatus(SubscriptionStatus.PAID);
        ipoSubscription.setPaidAt(LocalDateTime.now(clock));
        // save() 호출 불필요, update과이므로 더티체킹으로 두기!
        return IpoSubscriptionResDTO.from(ipoSubscription);
    }

    @Transactional
    public IpoSubscriptionResDTO cancel(UUID subscriptionId) {
        IpoSubscription ipoSubscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("청약을 찾을 수 없습니다."));
        if (ipoSubscription.getStatus() != SubscriptionStatus.APPLIED) {
            throw new IllegalStateException("APPLIED 상태에서만 취소할 수 있습니다.");
        }
        ipoSubscription.setStatus(SubscriptionStatus.CANCELLED);
        ipoSubscription.setPaidAt(null);
        ipoSubscription.setCancelledAt(LocalDateTime.now(clock));
        return IpoSubscriptionResDTO.from(ipoSubscription);
    }

    @Transactional
    public IpoSubscriptionResDTO get(UUID subscriptionId) {
        IpoSubscription ipoSubscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("청약을 찾을 수 없습니다."));
        return IpoSubscriptionResDTO.from(ipoSubscription);
    }
}
