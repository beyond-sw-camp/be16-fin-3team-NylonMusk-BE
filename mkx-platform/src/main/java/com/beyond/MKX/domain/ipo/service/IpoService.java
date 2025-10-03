package com.beyond.MKX.domain.ipo.service;

import com.beyond.MKX.domain.ipo.dto.IpoCreateReqDTO;
import com.beyond.MKX.domain.ipo.dto.IpoListReqDTO;
import com.beyond.MKX.domain.ipo.dto.IpoReviewReqDTO;
import com.beyond.MKX.domain.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.entity.IpoStatus;
import com.beyond.MKX.domain.ipo.repository.IpoRepository;
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

//        공모 없는 상장이므로, priceOnListing 여기서 확정
        ipo.list(LocalDateTime.now(clock), ipoListReqDTO.getPriceOnListing());
        return ipo;
    }



}
