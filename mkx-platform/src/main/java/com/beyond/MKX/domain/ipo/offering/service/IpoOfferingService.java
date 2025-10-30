package com.beyond.MKX.domain.ipo.offering.service;

import com.beyond.MKX.domain.ipo.offering.dto.IpoOfferingListDTO;
import com.beyond.MKX.domain.ipo.offering.dto.IpoOfferingReqDTO;
import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.offering.dto.IpoOfferingResDTO;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.ipo.entity.IpoStatus;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingType;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.ipo.ipo.repository.IpoRepository;
import com.beyond.MKX.domain.ipo.subscription.entity.SubscriptionStatus;
import com.beyond.MKX.domain.ipo.subscription.repository.IpoSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class IpoOfferingService {
    private final IpoRepository ipoRepository;
    private final IpoOfferingRepository ipoOfferingRepository;
    private final IpoSubscriptionRepository subscriptionRepository;
    private final TradingLockService tradingLockService;
    private final Clock clock;

    /* 공모 생성 */
    @Transactional
    public IpoOffering create(UUID ipoId, IpoOfferingReqDTO offeringReqDTO) {
        if (ipoId == null) {
            throw new IllegalArgumentException("상장(ipo) 아이디는 필수 입력값입니다.");
        }

        // 1️⃣ 상장 검증 및 잠금
        Ipo ipo = ipoRepository.findByIdForUpdate(ipoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 IPO입니다."));

        // 2️⃣ 공모 타입 자동 판별 (상장 상태 + 유상증자 여부)
        IpoOfferingType type = IpoOfferingType.INITIAL;
        if (ipo.getStatus() == IpoStatus.LISTED) {
            type = offeringReqDTO.isRightsIssue()
                    ? IpoOfferingType.RIGHTS_ISSUE
                    : IpoOfferingType.FOLLOW_ON;
        }

        // 3️⃣ 상장 후 공모(recordDate 필수)
        if (ipo.getStatus() == IpoStatus.LISTED && offeringReqDTO.getRecordDate() == null) {
            throw new IllegalArgumentException("상장 후 공모(N차 공모)에서는 recordDate가 필수입니다.");
        }

        // 4️⃣ 이전 차수 미종결 확인
        Integer lastRoundNo = ipoOfferingRepository.findMaxRoundNo(ipo.getId());
        int nextRound = (lastRoundNo == null) ? 1 : lastRoundNo + 1;

        boolean priorUnfinished = ipoOfferingRepository.existsByIpo_IdAndIpoOfferingStatusIn(
                ipo.getId(),
                EnumSet.of(IpoOfferingStatus.SCHEDULED, IpoOfferingStatus.OPEN)
        );
        if (priorUnfinished) {
            throw new IllegalStateException("이전 공모 차수가 아직 종결되지 않았습니다.");
        }

        // 5️⃣ IPO 자체 검증
        if (!Boolean.TRUE.equals(ipo.getIsOffering())) {
            throw new IllegalStateException("해당 IPO는 공모 미사용(isOffering=false)으로 설정되어 있습니다.");
        }
        if (ipo.getStatus() == IpoStatus.REJECTED) {
            throw new IllegalStateException("심사 거절된 IPO에서는 공모를 생성할 수 없습니다.");
        }

        // 6️⃣ 수치 검증
        if (offeringReqDTO.getOfferQuantity() <= 0 || offeringReqDTO.getLotSize() <= 0) {
            throw new IllegalArgumentException("공모 물량/청약 단위는 양수여야 합니다.");
        }
        if (offeringReqDTO.getOfferQuantity() % offeringReqDTO.getLotSize() != 0) {
            throw new IllegalArgumentException("공모 물량은 청약 단위의 배수여야 합니다.");
        }
        if (offeringReqDTO.getPriceBandMin() <= 0 ||
                offeringReqDTO.getPriceBandMax() <= 0 ||
                offeringReqDTO.getPriceBandMin() > offeringReqDTO.getPriceBandMax()) {
            throw new IllegalArgumentException("희망 공모가 밴드가 유효하지 않습니다.");
        }

        long faceValue = ipo.getFaceValue();
        if (faceValue > 0 && offeringReqDTO.getPriceBandMin() < faceValue) {
            throw new IllegalArgumentException("희망 공모가 최솟값은 액면가 이상이어야 합니다.");
        }

        // 7️⃣ 공모 엔티티 생성
        IpoOffering ipoOffering = IpoOffering.builder()
                .ipo(ipo)
                .roundNo(nextRound)
                .offeringType(type)
                .offerQuantity(offeringReqDTO.getOfferQuantity())
                .recordDate(offeringReqDTO.getRecordDate())
                .lotSize(offeringReqDTO.getLotSize())
                .priceBandMin(offeringReqDTO.getPriceBandMin())
                .priceBandMax(offeringReqDTO.getPriceBandMax())
                .offerPrice(null) // 생성 단계에서는 확정공모가 없음
                .subscriptionStart(offeringReqDTO.getSubscriptionStart())
                .subscriptionEnd(offeringReqDTO.getSubscriptionEnd())
                .allocationDate(offeringReqDTO.getAllocationDate())
                .refundDate(offeringReqDTO.getRefundDate())
                .depositRate(offeringReqDTO.getDepositRate())
                .competitionRatio(BigDecimal.ZERO)
                .ipoOfferingStatus(IpoOfferingStatus.SCHEDULED)
                .bookBuildingStart(offeringReqDTO.getBookBuildingStart())
                .bookBuildingEnd(offeringReqDTO.getBookBuildingEnd())
                .build();

        // 8️⃣ DB에 저장 (영속 상태로 만들어야 거래정지 로직 실행 가능)
        ipoOfferingRepository.save(ipoOffering);

        // 9️⃣ Follow-on / Rights Issue일 경우 거래 정지 처리
        if (type == IpoOfferingType.FOLLOW_ON || type == IpoOfferingType.RIGHTS_ISSUE) {
            tradingLockService.suspendTradingForOffering(ipoOffering);
        }

        return ipoOffering;
    }

    private void validateSchedule(
            java.time.LocalDateTime start, java.time.LocalDateTime end,
            LocalDate allocationDate, LocalDate refundDate
    ) {
        if (start == null || end == null || allocationDate == null || refundDate == null) {
            throw new IllegalArgumentException("일정 필드는 모두 필수입니다.");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("청약 시작시간은 마감시간보다 앞서야 합니다.");
        }
        LocalDate endD = end.toLocalDate();
        if (allocationDate.isBefore(endD)) {
            throw new IllegalArgumentException("배정일은 청약 마감일(날짜) 이후여야 합니다.");
        }
        if (refundDate.isBefore(allocationDate)) {
            throw new IllegalArgumentException("환불일은 배정일 이후여야 합니다.");
        }
    }

    private void requirePercent(BigDecimal v, String field) {
        if (v == null) throw new IllegalArgumentException(field + "는 필수입니다.");
        if (v.scale() > 2) {
            throw new IllegalArgumentException(field + "는 소수점 둘째 자리까지만 허용됩니다.");
        }
        if (v.compareTo(BigDecimal.ZERO) < 0 || v.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException(field + "는 0.00~100.00(%) 범위여야 합니다.");
        }
    }

    /* 공모가 확정 */
    @Transactional
    public IpoOffering fixOfferPrice(UUID offeringId, long offerPrice) {
        if (offerPrice <= 0) {
            throw new IllegalArgumentException("확정 공모가는 양수여야 합니다.");
        }

        IpoOffering ipoOffering = ipoOfferingRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공모입니다."));

        if (!Boolean.TRUE.equals(ipoOffering.getIpo().getIsOffering())) {
            throw new IllegalArgumentException("해당 IPO는 공모 미사용(isOffering-false)입니다.");
        }

        if (ipoOffering.getIpoOfferingStatus() != IpoOfferingStatus.CLOSED) {
            throw new IllegalArgumentException("확정 공모가(offerPrice)는 청약 마감(CLOSED) 상태에서만 확정할 수 있습니다.");
        }

        Long faceValue = ipoOffering.getIpo().getFaceValue();
        Long min = ipoOffering.getPriceBandMin();
        Long max = ipoOffering.getPriceBandMax();
        if (offerPrice < faceValue) {
            throw new IllegalArgumentException("확정 공모가는 액면가 이상이어야 합니다. (offerPrice = " + offerPrice + ", faceValue = " + faceValue + ")");
        }
        if (min == null || max == null || min <= 0 || max <= 0 || min > max) {
            throw new IllegalArgumentException("희망 공모가 범위가 유효하지 않습니다.");
        }
        if (offerPrice < min || offerPrice > max) {
            throw new IllegalArgumentException("확정 공모가는 밴드 범위(" + min + " ~ " + max + ") 내여야 합니다.");
        }

        // 확정
        ipoOffering.fixOfferPrice(offerPrice, min, max, faceValue);

        return ipoOffering;
    }

    /** SCHEDULED -> OPEN */
    @Transactional
    public IpoOffering open(UUID offeringId) {
        IpoOffering ipoOffering = ipoOfferingRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공모입니다."));

        if (ipoOffering.getIpoOfferingStatus() != IpoOfferingStatus.PRICE_FIXED) {
            throw new IllegalArgumentException("PRICE_FIXED 상태에서만 OPEN 할 수 있습니다.");
        }

        boolean checkNotClosed = ipoOfferingRepository.existsByIpo_IdAndRoundNoLessThanAndIpoOfferingStatusNotIn(
                ipoOffering.getIpo().getId(),
                ipoOffering.getRoundNo(),
                java.util.List.of(IpoOfferingStatus.CLOSED, IpoOfferingStatus.CANCELLED)
        );
        if (checkNotClosed) {
            throw new IllegalArgumentException("이전 차수가 종료되지 않아 OPEN 할 수 없습니다.");
        }


        validateSchedule(ipoOffering.getSubscriptionStart(), ipoOffering.getSubscriptionEnd(),
                ipoOffering.getAllocationDate(), ipoOffering.getRefundDate());
        requirePercent(ipoOffering.getDepositRate(), "depositRate");

        // 동일 IPO에 이미 OPEN 존재 금지(경합 방지 위해 잠금 쿼리 사용)
        long openCnt = ipoOfferingRepository.countOpenForIpoForUpdate(ipoOffering.getIpo().getId());
        if (openCnt > 0) {
            throw new IllegalStateException("동일 IPO에 이미 OPEN 상태 공모가 존재합니다.");
        }

        // 시간 조건(권장): 개시 시각 도달 확인
        var now = LocalDateTime.now(clock);
        if (now.isBefore(ipoOffering.getSubscriptionStart())) {
            throw new IllegalStateException("청약 시작 시각 전에는 개시할 수 없습니다.");
        }
        if (!now.isBefore(ipoOffering.getSubscriptionEnd())) {
            throw new IllegalStateException("청약 마감 시각을 지나 개시할 수 없습니다.");
        }

        ipoOffering.offeringOpen(now);
        return ipoOffering;
    }

    /** OPEN -> CLOSED */
    @Transactional
    public IpoOffering close(UUID offeringId) {
        IpoOffering ipoOffering = ipoOfferingRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공모입니다."));

        if (ipoOffering.getIpoOfferingStatus() != IpoOfferingStatus.OPEN) {
            throw new IllegalStateException("OPEN 상태에서만 마감할 수 있습니다.");
        }

        var now = LocalDateTime.now(clock);
        // 권장: 마감 시각 도달 확인(운영자 강제 마감 허용 시 이 체크 완화 가능)
        if (now.isBefore(ipoOffering.getSubscriptionEnd())) {
            throw new IllegalStateException("청약 마감 시각 이전에는 마감할 수 없습니다.");
        }

        ipoOffering.offeringCloseNow(now);
        return ipoOffering;
    }

    /** SCHEDULED/OPEN -> CANCELLED */
    @Transactional
    public IpoOffering cancel(UUID offeringId) {
        IpoOffering ipoOffering = ipoOfferingRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공모입니다."));

        IpoOfferingStatus st = ipoOffering.getIpoOfferingStatus();
        if (st != IpoOfferingStatus.SCHEDULED && st != IpoOfferingStatus.OPEN) {
            throw new IllegalStateException("SCHEDULED 또는 OPEN 상태에서만 취소할 수 있습니다.");
        }

        // 선택: OPEN 취소 시 참여분 환불/정산 사전 조건 확인(타 모듈 이벤트/검증 연동)
        // 예: if (st == OPEN && subscriptionService.existsPaid(...)) throw ...;

        ipoOffering.offeringCancel();
        return ipoOffering;
    }

    @Transactional(readOnly = true)
    public IpoOfferingResDTO findById(UUID ipoOfferingId) {
        IpoOffering ipoOffering = ipoOfferingRepository.findById(ipoOfferingId)
                .orElseThrow(() -> new IllegalArgumentException("찾는 공모가 없습니다."));
        return IpoOfferingResDTO.from(ipoOffering);
    }

    @Transactional(readOnly = true)
    public Page<IpoOfferingListDTO> listByIpo(UUID ipoId, Collection<IpoOfferingStatus> statuses, Pageable pageable) {
        Page<IpoOffering> page = (statuses==null || statuses.isEmpty())
                ? ipoOfferingRepository.findByIpo_Id(ipoId, pageable)
                : ipoOfferingRepository.findByIpo_IdAndIpoOfferingStatusIn(ipoId, statuses, pageable);
        return page.map(e -> IpoOfferingListDTO.from(e, clock));
    }

    @Transactional(readOnly = true)
    public Page<IpoOfferingListDTO> listAll(Collection<IpoOfferingStatus> statuses, boolean onlySubscribable, Pageable pageable) {
        Page<IpoOffering> page;
        if (onlySubscribable) {
            page = ipoOfferingRepository.findCurrentlySubscribable(LocalDateTime.now(clock), pageable);
        } else if (statuses==null || statuses.isEmpty()) {
            page = ipoOfferingRepository.findAll(pageable);
        } else {
            page = ipoOfferingRepository.findByIpoOfferingStatusIn(statuses, pageable);
        }
        return page.map(e -> IpoOfferingListDTO.from(e, clock));
    }

    @Transactional
    public IpoOffering startBookBuilding(UUID offeringId) {
        IpoOffering offering = ipoOfferingRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모 없음"));
        if (offering.getIpoOfferingStatus() != IpoOfferingStatus.SCHEDULED)
            throw new IllegalStateException("SCHEDULED 상태에서만 수요예측 시작 가능");
        offering.setIpoOfferingStatus(IpoOfferingStatus.BOOK_BUILDING);
        return offering;
    }
}
