package com.beyond.MKX.domain.ipo.ipo.service;

import com.beyond.MKX.domain.ipo.ipo.dto.IpoCreateReqDTO;
import com.beyond.MKX.domain.ipo.ipo.dto.IpoListReqDTO;
import com.beyond.MKX.domain.ipo.ipo.dto.IpoReviewReqDTO;
import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.ipo.entity.IpoStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.ipo.ipo.repository.IpoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IpoService {
    private final IpoRepository ipoRepository;
    private final IpoOfferingRepository ipoOfferingRepository;
    private Clock clock = Clock.systemDefaultZone();

//    1. 기업 상장 요청 (requested)
    @Transactional
    public Ipo createRequest(IpoCreateReqDTO ipoCreateReqDTO) {
        if (ipoRepository.existsBySymbol(ipoCreateReqDTO.getSymbol())) {
            throw new IllegalArgumentException("이미 사용중인 종목 이름입니다." + ipoCreateReqDTO.getSymbol());
        }
        Ipo ipo = ipoCreateReqDTO.toEntity();
        return ipoRepository.save(ipo);
    }

//    2. 거래소 관리자 심사(under review)
    @Transactional
    public Ipo adminReview(UUID ipoId, IpoReviewReqDTO ipoReviewReqDTO) {
        Ipo ipo = ipoRepository.findById(ipoId)
                .orElseThrow(() -> new IllegalArgumentException("요청된 IPO가 없습니다."));

        ipo.markUnderReview();

        if (Boolean.TRUE.equals(ipoReviewReqDTO.getApprove())) {
            ipo.approve();
        } else {
            if (ipoReviewReqDTO.getRejectReason() == null || ipoReviewReqDTO.getRejectReason().isBlank()) {
                throw new IllegalArgumentException("반려 사유는 필수입니다.");
            }
            ipo.reject(ipoReviewReqDTO.getRejectReason());
        }
        return ipo;
    }

//    3. 거래소 관리자 상장 확정(listing)
    @Transactional
    public Ipo list(UUID ipoId, IpoListReqDTO ipoListReqDTO) {
        Ipo ipo = ipoRepository.findById(ipoId).orElseThrow(() -> new IllegalArgumentException("심사된 IPO가 없습니다."));

        if (ipo.getStatus() != IpoStatus.APPROVED) {
            throw new IllegalArgumentException("승인(APPROVED)된 건만 상장 확정할 수 있습니다.");
        }

        if (Boolean.TRUE.equals(ipo.getIsOffering())) {
            // 공모 필수 경로: 공모가 존재하고, 배정(또는 정산) 완료 상태여야 상장 가능
            boolean hasOffering = ipoOfferingRepository.existsByIpo_Id(ipoId);
            if (!hasOffering) throw new IllegalStateException("공모가 필요한 상장입니다. 공모가 생성되지 않았습니다.");

            boolean done = ipoOfferingRepository.existsByIpo_IdAndIpoOfferingStatusIn(
                    ipoId, java.util.List.of(IpoOfferingStatus.ALLOCATED, IpoOfferingStatus.SETTLED)
            );
            if (!done) throw new IllegalStateException("공모 절차(배정/정산)가 완료되지 않아 상장할 수 없습니다.");

            // 통상 상장 기준가는 확정 공모가를 사용
            IpoOffering last = ipoOfferingRepository.findTopByIpo_IdOrderByRoundNoDesc(ipoId)
                    .orElseThrow(() -> new IllegalStateException("공모 정보가 없습니다."));
            if (last.getOfferPrice() == null) {
                throw new IllegalStateException("확정 공모가가 없어 상장 기준가를 결정할 수 없습니다.");
            }
            ipo.list(LocalDateTime.now(clock), last.getOfferPrice());

        } else {
            // 공모 미사용 경로: 바로 상장 가능(요청 값으로 기준가 세팅)
            ipo.list(LocalDateTime.now(clock), ipoListReqDTO.getPriceOnListing());
        }

        return ipo;
    }



}
