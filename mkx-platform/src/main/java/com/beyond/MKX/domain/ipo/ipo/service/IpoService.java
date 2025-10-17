package com.beyond.MKX.domain.ipo.ipo.service;

import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.common.auth.security.GatewayHeaderAuthFilter;
import com.beyond.MKX.common.s3.S3Manager;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
import com.beyond.MKX.domain.ipo.ipo.dto.*;
import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.ipo.entity.IpoStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.ipo.ipo.repository.IpoRepository;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import com.beyond.MKX.domain.stock.util.StockTickerGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IpoService {
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final IpoRepository ipoRepository;
    private final IpoOfferingRepository ipoOfferingRepository;
    private final CorporationRepository corporationRepository;
    private final StockRepository stockRepository;
    private final S3Manager s3Manager;
    private final AdminRepository adminRepository;
    private Clock clock = Clock.systemDefaultZone();
    private static final int TICKER_GEN_MAX_ATTEMPTS = 20;

//    1. 기업 상장 요청 (requested)
    @Transactional
    public IpoCreateResDTO createRequest(IpoCreateReqDTO ipoCreateReqDTO) {
        if (ipoRepository.existsBySymbol(ipoCreateReqDTO.getSymbol())) {
            throw new IllegalArgumentException("이미 사용중인 종목 이름입니다." + ipoCreateReqDTO.getSymbol());
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomAdminPrincipal customAdminPrincipal)) {
            throw new IllegalArgumentException("인증되지 않은 요청입니다.");
        }
        var admin = adminRepository.findById(customAdminPrincipal.id())
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));
        var corp = admin.getCorporation();
        if (corp == null) {
            throw new IllegalArgumentException("해당 관리자 계정에 연결된 기업이 없습니다.");
        }

        Corporation corporation = corporationRepository.findById(corp.getId())
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
    public IpoListResDTO list(UUID ipoId, Long requestedPriceOnListing) {
        Ipo ipo = ipoRepository.findByIdForUpdate(ipoId)
                .orElseThrow(() -> new IllegalArgumentException("해당 IPO(ID: %s)를 찾을 수 없습니다.".formatted(ipoId))); // ← 동시 상장 시도 직격 차단
        if (ipo.getStatus() == IpoStatus.LISTED) {
            // 멱등 처리: 이미 상장된 경우 기존 스냅샷 기반으로 DTO 반환 or 에러
            // return IpoListResDTO.of(ipo, existingTicker, issued);
            throw new IllegalStateException("이미 상장 처리된 건입니다.");
        }
        if (ipo.getStatus() != IpoStatus.APPROVED) {
            throw new IllegalArgumentException("승인(APPROVED)된 건만 상장 확정할 수 있습니다.");
        }
        Long pre = Optional.ofNullable(ipo.getPreIpoOutstandingShares()).orElse(0L);
        Long issued = 0L;

        if (Boolean.TRUE.equals(ipo.getIsOffering())) {
            boolean hasOffering = ipoOfferingRepository.existsByIpo_Id(ipoId);
            if (!hasOffering) throw new IllegalStateException("공모가 필요한 상장입니다. 공모가 생성되지 않았습니다.");

            boolean done = ipoOfferingRepository.existsByIpo_IdAndIpoOfferingStatusIn(
                    ipoId, List.of(IpoOfferingStatus.SETTLED)
            );
            if (!done) throw new IllegalStateException("공모 절차(배정/정산)가 완료되지 않아 상장할 수 없습니다.");
            // 공모 경로: 확정 공모가 사용, 요청 바디의 priceOnListing은 무시
            IpoOffering last = ipoOfferingRepository.findTopByIpo_IdOrderByRoundNoDesc(ipoId)
                    .orElseThrow(() -> new IllegalStateException("공모 정보가 없습니다."));
            if (last.getOfferPrice() == null) {
                throw new IllegalStateException("확정 공모가가 없어 상장 기준가를 결정할 수 없습니다.");
            }
            issued = Optional.ofNullable(last.getIssuedQuantity()).orElse(0L);
            long totalSharesAtListing = Math.addExact(pre, issued);

            ipo.list(LocalDateTime.now(clock), last.getOfferPrice());
            ipo.updateOutstandingSharesAtListing(totalSharesAtListing);

        } else {
            // 비공모 경로: 요청 바디의 priceOnListing 필수
            if (requestedPriceOnListing == null || requestedPriceOnListing <= 0) {
                throw new IllegalArgumentException("비공모 상장에서는 priceOnListing(양수)이 필수입니다.");
            }
            ipo.list(LocalDateTime.now(clock), requestedPriceOnListing);
            ipo.updateOutstandingSharesAtListing(pre);
        }

        // --- 여기서 TICKER 생성 + Stock 1회 생성 ---
        final UUID corporationId = ipo.getCorporation().getId();
        final String nameKo = ipo.getSymbol(); // 종목명(한글명)

        // 총 발행 주식 수: list()가 세팅한 값을 사용
        Long totalSharesAtListing = ipo.getOutstandingSharesAtListing();
        if (totalSharesAtListing == null) {
            throw new IllegalStateException("상장 시점 총 발행 주식 수가 계산되지 않았습니다.");
        }

        // 이미 종목이 존재하면 재생성하지 않음
        // (선택) ipoId 기준으로 한 번만 만들고 싶다면 StockRepository에 findByCorporationIdAndTicker 대신 findByIpo_Id 등을 사용
        String ticker = null;
        for (int attempt = 0; attempt < TICKER_GEN_MAX_ATTEMPTS; attempt++) {
            String candidate = StockTickerGenerator.generate(corporationId, attempt);
            try {
                Stock stock = Stock.builder()
                        .corporationId(corporationId)
                        .ticker(candidate)
                        .nameKo(nameKo)
                        .status(Stock.Status.LISTED)
                        .totalSharesOutstanding(totalSharesAtListing)
                        .build();
                stockRepository.saveAndFlush(stock); // 즉시 반영해 충돌 조기 감지
                ticker = candidate;
                break; // 성공
            } catch (org.springframework.dao.DataIntegrityViolationException dup) {
                // uk_stock_ticker 충돌 → 다음 attempt
            }
        }
        if (ticker == null) {
            throw new IllegalStateException("종목코드 생성에 실패했습니다. 잠시 후 다시 시도하세요.");
        }
        return IpoListResDTO.of(ipo, ticker, issued);
    }

    @Transactional(readOnly = true)
    public IpoDetailDTO getDetailById(UUID ipoId) {
        Ipo ipo = ipoRepository.findById(ipoId)
                .orElseThrow(() -> new IllegalArgumentException("IPO를 찾을 수 없습니다."));
        return IpoDetailDTO.of(ipo);
    }

    @Transactional(readOnly = true)
    public IpoDetailDTO getMyIpo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomAdminPrincipal customAdminPrincipal)) {
            throw new IllegalArgumentException("인증되지 않은 요청입니다.");
        }
        
        var admin = adminRepository.findById(customAdminPrincipal.id())
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));
        var corp = admin.getCorporation();
        if (corp == null) {
            throw new IllegalArgumentException("해당 관리자 계정에 연결된 기업이 없습니다.");
        }

        Ipo ipo = ipoRepository.findByCorporationIdOrderByRequestedAtDesc(corp.getId())
                .stream()
                .findFirst()
                .orElse(null);
        
        if (ipo == null) {
            throw new IllegalArgumentException("해당 기업의 IPO 정보가 없습니다.");
        }
        
        return IpoDetailDTO.of(ipo);
    }



}
