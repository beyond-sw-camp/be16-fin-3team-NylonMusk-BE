package com.beyond.MKX.domain.ipo.ipo.service;

import com.beyond.MKX.common.s3.S3Manager;
import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
import com.beyond.MKX.domain.ipo.ipo.dto.IpoCreateReqDTO;
import com.beyond.MKX.domain.ipo.ipo.dto.IpoCreateResDTO;
import com.beyond.MKX.domain.ipo.ipo.dto.IpoListReqDTO;
import com.beyond.MKX.domain.ipo.ipo.dto.IpoReviewReqDTO;
import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.ipo.entity.IpoStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.ipo.ipo.repository.IpoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IpoService {
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final IpoRepository ipoRepository;
    private final IpoOfferingRepository ipoOfferingRepository;
    private final CorporationRepository corporationRepository;
    private final S3Manager s3Manager;
    private Clock clock = Clock.systemDefaultZone();

//    1. 기업 상장 요청 (requested)
    @Transactional
    public IpoCreateResDTO createRequest(IpoCreateReqDTO ipoCreateReqDTO) {
        if (ipoRepository.existsBySymbol(ipoCreateReqDTO.getSymbol())) {
            throw new IllegalArgumentException("이미 사용중인 종목 이름입니다." + ipoCreateReqDTO.getSymbol());
        }
        Corporation corporation = corporationRepository.findById(ipoCreateReqDTO.getCorporation().getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업입니다."));

        Ipo ipo = ipoCreateReqDTO.toEntity(corporation);
        ipoRepository.save(ipo);

        if (ipoCreateReqDTO.getPreShareholdersFile() == null || ipoCreateReqDTO.getPreShareholdersFile().isEmpty()) {
            throw new IllegalArgumentException("주주 명부 파일이 비어 있습니다.");
        }
        if (ipoCreateReqDTO.getFinancialStatements() == null || ipoCreateReqDTO.getFinancialStatements().isEmpty()) {
            throw new IllegalArgumentException("재무제표 파일이 비어 있습니다.");
        }

//        S3 업로드 - 보상삭제포함
        String base = "ipo/" + ipo.getId();
        String preHoldersUrl = null;
        String financialUrl = null;

        try {
            preHoldersUrl = s3Manager.upload(ipoCreateReqDTO.getPreShareholdersFile(), base + "/pre-shareholders");
            financialUrl = s3Manager.upload(ipoCreateReqDTO.getFinancialStatements(), base + "/financials");
        } catch (RuntimeException exception) {
            if (preHoldersUrl != null) {
                try { s3Manager.delete(preHoldersUrl); } catch (Exception ignored) {}
            }
            // 트랜잭션 롤백으로 방금 INSERT된 IPO도 취소됩니다.
            throw exception;
        }

//        엔터티에 파일 URL 세팅 (더티체킹으로 커밋 시 UPDATE발생)
        ipo.updatePreShareholdersFileUrl(preHoldersUrl);
        ipo.updateFinancialStatementsUrl(financialUrl);

        return IpoCreateResDTO.from(ipo);
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
