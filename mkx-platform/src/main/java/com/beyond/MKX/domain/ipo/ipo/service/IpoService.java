package com.beyond.MKX.domain.ipo.ipo.service;

import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.common.auth.security.GatewayHeaderAuthFilter;
import com.beyond.MKX.common.s3.S3Manager;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.entity.OutboxStatus;
import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.repository.IpoAllocationOutboxRepository;
import com.beyond.MKX.domain.ipo.ipo.dto.*;
import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.ipo.entity.IpoStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.ipo.ipo.repository.IpoRepository;
import com.beyond.MKX.domain.ipo.subscription.entity.IpoSubscription;
import com.beyond.MKX.domain.ipo.subscription.entity.SubscriptionStatus;
import com.beyond.MKX.domain.ipo.subscription.repository.IpoSubscriptionRepository;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import com.beyond.MKX.domain.stock.util.StockTickerGenerator;
import com.beyond.MKX.domain.ipo.client.NewsRemappingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
@Slf4j
public class IpoService {
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final IpoRepository ipoRepository;
    private final IpoOfferingRepository ipoOfferingRepository;
    private final IpoSubscriptionRepository subscriptionRepository;
    private final CorporationRepository corporationRepository;
    private final StockRepository stockRepository;
    private final IpoAllocationOutboxRepository outboxRepository;
    private final com.beyond.MKX.domain.financial.service.FinancialUploadService financialUploadService;
    private final IpoAllocationFeign orderingFeign;
    private final S3Manager s3Manager;
    private final AdminRepository adminRepository;
    private final NewsRemappingClient newsRemappingClient;
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

        boolean hasActiveIpo = ipoRepository.existsByCorporation_IdAndStatusIn(
                corporation.getId(),
                List.of(IpoStatus.REQUESTED, IpoStatus.UNDER_REVIEW, IpoStatus.APPROVED, IpoStatus.LISTED)
                );

        if (hasActiveIpo) {
            throw new IllegalArgumentException("이미 상장이 진행됐거나 진행 중인 기업입니다. 진행 중이라면 기존 요청을 먼저 처리하세요.");
        }

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
                try {
                    s3Manager.delete(preHoldersUrl);
                } catch (Exception ignored) {
                }
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
    /**
     * 상장 확정 + Stock 생성 + 커밋 후 보유 주식 반영 전송
     */
    @Transactional
    public IpoListResDTO list(UUID ipoId, Long requestedPriceOnListing) {
        // 1) IPO 잠금 + 가드
        Ipo ipo = ipoRepository.findByIdForUpdate(ipoId)
                .orElseThrow(() -> new IllegalArgumentException("IPO를 찾을 수 없습니다."));
        if (ipo.getStatus() == IpoStatus.LISTED) throw new IllegalStateException("이미 상장됨.");
        if (ipo.getStatus() != IpoStatus.APPROVED) throw new IllegalArgumentException("APPROVED만 상장 가능.");

        // 2) 공모 여부 따라 상장가/총주식수 확정
        Long pre = Optional.ofNullable(ipo.getPreIpoOutstandingShares()).orElse(0L);
        Long issued = 0L;

        if (Boolean.TRUE.equals(ipo.getIsOffering())) {
            boolean hasOffering = ipoOfferingRepository.existsByIpo_Id(ipoId);
            if (!hasOffering) throw new IllegalStateException("공모 필요 상장인데 공모가 없습니다.");

            boolean done = ipoOfferingRepository.existsByIpo_IdAndIpoOfferingStatusIn(
                    ipoId, List.of(IpoOfferingStatus.SETTLED));
            if (!done) throw new IllegalStateException("공모 정산(SETTLED) 전에는 상장 불가.");

            IpoOffering last = ipoOfferingRepository.findTopByIpo_IdOrderByRoundNoDesc(ipoId)
                    .orElseThrow(() -> new IllegalStateException("공모 정보가 없습니다."));
            if (last.getOfferPrice() == null) throw new IllegalStateException("확정 공모가 없음.");

            // 정산 미완료 청약 확인
            List<IpoSubscription> unsettledSubscriptions = subscriptionRepository.findAllByOfferingIdAndStatus(
                    last.getId(), SubscriptionStatus.ALLOCATED);
            if (!unsettledSubscriptions.isEmpty()) {
                throw new IllegalStateException(
                        String.format("정산이 완료되지 않은 청약이 %d건 있습니다. 모든 청약의 정산을 완료한 후 상장할 수 있습니다.", unsettledSubscriptions.size())
                );
            }

            issued = Optional.ofNullable(last.getIssuedQuantity()).orElse(0L);
            long total = Math.addExact(pre, issued);

            ipo.list(LocalDateTime.now(clock), last.getOfferPrice());
            ipo.updateOutstandingSharesAtListing(total);
        } else {
            if (requestedPriceOnListing == null || requestedPriceOnListing <= 0)
                throw new IllegalArgumentException("비공모 상장은 양수의 priceOnListing 필수.");
            ipo.list(LocalDateTime.now(clock), requestedPriceOnListing);
            ipo.updateOutstandingSharesAtListing(pre);
        }

        // 3) Stock 생성(티커 확정)
        final Corporation corporation = ipo.getCorporation();
        final UUID corpId = corporation.getId();
        final String nameKo = ipo.getSymbol();
        final long totalSharesAtListing = Optional.ofNullable(ipo.getOutstandingSharesAtListing())
                .orElseThrow(() -> new IllegalStateException("상장 시점 총 발행 주식 수 미계산."));
        final String stockImageUrl = corporation.getLogoUrl(); // 기업 로고를 종목 이미지로 사용

        String ticker = null;
        for (int attempt = 0; attempt < TICKER_GEN_MAX_ATTEMPTS; attempt++) {
            String candidate = StockTickerGenerator.generate(corpId, attempt);
            try {
                Stock stock = Stock.builder()
                        .corporationId(corpId)
                        .ticker(candidate)
                        .nameKo(nameKo)
                        .status(Stock.Status.LISTED)
                        .totalSharesOutstanding(totalSharesAtListing)
                        .imageUrl(stockImageUrl)
                        .build();
                stockRepository.saveAndFlush(stock);
                ipo.linkStock(stock.getId(), stock.getTicker());
                ticker = candidate;
                break;
            } catch (org.springframework.dao.DataIntegrityViolationException dup) {
                // 충돌 → 다음 후보
            }
        }
        if (ticker == null) throw new IllegalStateException("종목코드 생성 실패.");

        // 3-1) 상장 시 제출된 재무제표 저장: 연간 5개년 + 직전 분기 1개
        try {
            String fsUrl = ipo.getFinancialStatementsUrl();
            if (fsUrl != null && !fsUrl.isBlank()) {
                financialUploadService.uploadIpoFinancials(ipo.getStockId(), fsUrl);
            } else {
                log.info("[IPO-LISTING] 재무제표 파일 URL 없음. stockId={}", ipo.getStockId());
            }
        } catch (Exception ex) {
            // 상장 자체는 계속 진행, 재무 업로드 실패는 로그만 남김
            log.error("[IPO-LISTING] 연간 재무제표 저장 실패: stockId={}, error={}", ipo.getStockId(), ex.getMessage(), ex);
        }

        // 3-2) 상장된 종목의 기존 뉴스 재매핑
        try {
            newsRemappingClient.remapNewsForListedStock(
                ipo.getStockId().toString(),
                nameKo,
                ticker
            );
            log.info("[IPO-LISTING] 뉴스 재매핑 완료: stockId={}, ticker={}", 
                    ipo.getStockId(), ticker);
        } catch (Exception ex) {
            // 상장 자체는 계속 진행, 뉴스 재매핑 실패는 로그만 남김
            log.error("[IPO-LISTING] 뉴스 재매핑 실패: stockId={}, ticker={}, error={}", 
                    ipo.getStockId(), ticker, ex.getMessage(), ex);
        }

        // 4) 커밋 후: 보유 주식 반영(Outbox → Ordering 단건 PUT)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    UUID id = ipo.getId();
                    String t = ipo.getStockTicker();
                    Long price = ipo.getPriceOnListing();

                    log.info("[IPO-LISTING] 상장 후 주식 배정 시작. ipoId={}, ticker={}, price={}", id, t, price);

                    var events = outboxRepository.findAllByIpoIdAndStatus(id, OutboxStatus.PENDING);
                    log.info("[IPO-LISTING] 조회된 PENDING 이벤트 수: {}", events.size());

                    if (events.isEmpty()) {
                        log.warn("[IPO-LISTING] PENDING 상태의 이벤트가 없습니다!");
                        return;
                    }

                    int ok = 0, fail = 0;

                    for (var ev : events) {
                        log.info("[IPO-LISTING] 이벤트 처리 시작. allocationId={}, accountId={}, qty={}",
                                ev.getAllocationId(), ev.getAccountId(), ev.getQty());

                        var dto = StockUpdateDTO.builder()
                                .idempotencyKey(ev.getAllocationId())
                                .allocationId(ev.getAllocationId())
                                .offeringId(ev.getOfferingId())
                                .memberAccountId(ev.getAccountId())
                                .brokerageId(ev.getBrokerageId())
                                .ticker(t)
                                .qtyDelta(ev.getQty())
                                .unitPrice(price)
                                .reason("IPO_ALLOCATION")
                                .build();

                        try {
                            log.info("[IPO-LISTING] ordering-service 호출 시작");
                            orderingFeign.applyStockUpdate(dto);
                            ev.markSent();
                            ok++;
                            log.info("[IPO-LISTING] ordering-service 호출 성공");
                        } catch (Exception sendEx) {
                            ev.markFailed();
                            fail++;
                            log.error("[IPO-LISTING] ordering-service 호출 실패. allocationId={}, error={}",
                                    ev.getAllocationId(), sendEx.getMessage(), sendEx);
                        }
                    }

                    outboxRepository.saveAll(events);
                    log.info("[IPO-LISTING] 주식 배정 완료. 성공={}, 실패={}", ok, fail);

                } catch (Exception ex) {
                    log.error("[IPO-LISTING] 상장 후 주식 배정 중 예외 발생", ex);
                }
            }
        });
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
