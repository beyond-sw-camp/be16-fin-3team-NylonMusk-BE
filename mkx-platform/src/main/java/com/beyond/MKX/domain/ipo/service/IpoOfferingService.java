package com.beyond.MKX.domain.ipo.service;

import com.beyond.MKX.domain.ipo.dto.IpoOfferingReqDTO;
import com.beyond.MKX.domain.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.entity.IpoStatus;
import com.beyond.MKX.domain.ipo.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.ipo.repository.IpoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class IpoOfferingService {
    private final IpoRepository ipoRepository;
    private final IpoOfferingRepository ipoOfferingRepository;

    /* 공모 생성 */
    @Transactional
    public IpoOffering create(IpoOfferingReqDTO offeringReqDTO) {
        if (offeringReqDTO.getIpoId() == null) {
            throw new IllegalArgumentException("상장(ipo) 아이디는 필수 입력값입니다.");
        }
        // 1) 상장 존재/상태 검증
        Ipo ipo = ipoRepository.findById(offeringReqDTO.getIpoId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Ipo입니다."));
        // 공모 사용 여부 가드
        if (!Boolean.TRUE.equals(ipo.getIsOffering())) {
            throw new IllegalStateException("해당 IPO는 공모 미사용(isOffering=false)으로 설정되어 있어 공모를 생성할 수 없습니다.");
        }

        if (ipo.getStatus() == IpoStatus.LISTED || ipo.getStatus() == IpoStatus.REJECTED || ipo.getStatus() == IpoStatus.CANCELLED) {
            throw new IllegalArgumentException("해당 상장(ipo) 상태에서는 공모를 생성할 수 없습니다." + ipo.getStatus());
        }
        // 2) 공모 차수 중복 방지 (ipo_id, round_no)
        if (ipoOfferingRepository.existsByIpo_IdAndRoundNo(ipo.getId(), offeringReqDTO.getRoundNo())) {
            throw new IllegalArgumentException("이미 존재하는 공모 차수입니다. (roundNo = " + offeringReqDTO.getRoundNo() + ")");
        }
        // 3) 값 / 일정 / 최소*최대 검증
        if (offeringReqDTO.getOfferQuantity() <= 0 || offeringReqDTO.getLotSize() <= 0) {
            throw new IllegalArgumentException("공모 물량/청약 단위는 양수여야 합니다.");
        }
        if (offeringReqDTO.getOfferQuantity() % offeringReqDTO.getLotSize() != 0) {
            throw new IllegalArgumentException("공모 물량은 청약 단위의 배수여야 합니다.");
        }
        if (offeringReqDTO.getPriceBandMin() <= 0 || offeringReqDTO.getPriceBandMax() <= 0 || offeringReqDTO.getPriceBandMin() > offeringReqDTO.getPriceBandMax()) {
            throw new IllegalArgumentException("희망 공모가 범위가 유효하지 않습니다.");
        }

        IpoOffering ipoOffering = IpoOffering.builder()
                .ipo(ipo)
                .roundNo(offeringReqDTO.getRoundNo())
                .offerQuantity(offeringReqDTO.getOfferQuantity())
                .lotSize(offeringReqDTO.getLotSize())
                .priceBandMin(offeringReqDTO.getPriceBandMin())
                .priceBandMax(offeringReqDTO.getPriceBandMax())
                .offerPrice(null) // 생성 단계에서는 확정 공모가 없음
                .subscriptionStart(offeringReqDTO.getSubscriptionStart())
                .subscriptionEnd(offeringReqDTO.getSubscriptionEnd())
                .allocationDate(offeringReqDTO.getAllocationDate())
                .refundDate(offeringReqDTO.getRefundDate())
                .depositRate(offeringReqDTO.getDepositRate())
                .capRatio(offeringReqDTO.getCapRatio())
                .competitionRatio(BigDecimal.ZERO)
                .ipoOfferingStatus(IpoOfferingStatus.SCHEDULED)
                .build();

        return ipoOfferingRepository.save(ipoOffering);
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
}
