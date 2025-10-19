package com.beyond.MKX.domain.disclosure.service;

import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.common.s3.S3Manager;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.entity.Role;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.disclosure.dto.DisclosureRegisterReqFormDto;
import com.beyond.MKX.domain.disclosure.dto.DuplicateDisclosureInfo;
import com.beyond.MKX.domain.disclosure.dto.DisclosureRevisionReqFormDto;
import com.beyond.MKX.domain.disclosure.dto.DisclosureAdditionalReqFormDto;
import com.beyond.MKX.domain.disclosure.entity.Disclosure;
import com.beyond.MKX.domain.disclosure.entity.DisclosureStatus;
import com.beyond.MKX.domain.disclosure.entity.DisclosureRelationType;
import com.beyond.MKX.domain.disclosure.repository.DisclosureRepository;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import com.beyond.MKX.common.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisclosureService {

    private final DisclosureRepository disclosureRepository;
    private final StockRepository stockRepository;
    private final AdminRepository adminRepository;
    private final S3Manager s3Manager;

    /** 공시 등록 - Multipart 파일 업로드 포함 */
    @Transactional
    public Disclosure register(DisclosureRegisterReqFormDto request) {
        // 1) 인증된 기업 관리자 확인 + 본인 소속 기업의 상장 종목인지 확인
        validateListedStockOwnership(request.getStockId());

        // 2) 최근 중복 제목 가드 (기본 10분)
        if (!Boolean.TRUE.equals(request.getForce())) {
            String titleNorm = request.getTitle() == null ? null : request.getTitle().trim();
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
            disclosureRepository.findRecentDuplicate(request.getStockId(), titleNorm, threshold)
                    .ifPresent(dup -> {
                        throw new DuplicateResourceException(
                                "최근 동일 제목 공시가 제출되었습니다.",
                                new DuplicateDisclosureInfo(dup.getId(), dup.getTitle(), dup.getCreatedAt())
                        );
                    });
        }

        // 3) 파일 업로드: disclosures/pending/{type}/{yyyy}/{MM}/{stockId}
        String type = request.getDisclosureType().name().toLowerCase();
        LocalDate today = LocalDate.now();
        String prefix = String.format("disclosures/pending/%s/%04d/%02d/%s",
                type, today.getYear(), today.getMonthValue(), request.getStockId());
        String fileUrl = uploadOrThrow(request.getFile(), prefix);

        // 4) 공시 저장
        Disclosure disclosure = Disclosure.builder()
                .stockId(request.getStockId())
                .disclosureType(request.getDisclosureType())
                .title(request.getTitle())
                .summary(request.getSummary())
                .fileUrl(fileUrl)
                .status(DisclosureStatus.PENDING)
                .stockNameSnapshot(request.getStockNameSnapshot())
                .tickerSnapshot(request.getTickerSnapshot())
                .build();
        return disclosureRepository.save(disclosure);
    }

    /** 공시 파일 재업로드 - Multipart */
    @Transactional
    public Disclosure updateFile(UUID id, MultipartFile file, String newSummary) {
        Disclosure disclosure = disclosureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공시를 찾을 수 없습니다."));

        // 해당 공시의 종목이 여전히 상장 상태이며, 요청자가 소유한 기업의 종목인지 확인
        validateListedStockOwnership(disclosure.getStockId());

        String oldUrl = disclosure.getFileUrl();
        // disclosures/pending/{type}/{yyyy}/{MM}/{stockId}
        String type = disclosure.getDisclosureType().name().toLowerCase();
        LocalDate today = LocalDate.now();
        String prefix = String.format("disclosures/pending/%s/%04d/%02d/%s",
                type, today.getYear(), today.getMonthValue(), disclosure.getStockId());
        String newUrl = uploadOrThrow(file, prefix);

        disclosure.updateFile(newUrl, newSummary);

        if (oldUrl != null && !oldUrl.isBlank()) {
            try { s3Manager.delete(oldUrl); } catch (Exception ignored) {}
        }
        return disclosure;
    }

    /** 승인된 공시 정정 등록 (새로운 row 생성) */
    @Transactional
    public Disclosure revise(UUID baseId, DisclosureRevisionReqFormDto request) {
        Disclosure base = disclosureRepository.findById(baseId)
                .orElseThrow(() -> new IllegalArgumentException("공시를 찾을 수 없습니다."));
        if (base.getStatus() != DisclosureStatus.APPROVED) {
            throw new IllegalStateException("승인된 공시만 정정할 수 있습니다.");
        }

        // 소유/상장 검증
        validateListedStockOwnership(base.getStockId());

        // 그룹 루트 및 다음 회차 산정
        UUID rootId = base.getOriginId() != null ? base.getOriginId() : base.getId();
        Integer maxRev = disclosureRepository.findMaxRevisionInGroup(rootId);
        int nextRev = (maxRev == null ? 0 : maxRev) + 1;

        // 파일 업로드(pending)
        String type = base.getDisclosureType().name().toLowerCase();
        LocalDate today = LocalDate.now();
        String prefix = String.format("disclosures/pending/%s/%04d/%02d/%s",
                type, today.getYear(), today.getMonthValue(), base.getStockId());
        String fileUrl = uploadOrThrow(request.getFile(), prefix);

        Disclosure revision = Disclosure.builder()
                .stockId(base.getStockId())
                .disclosureType(base.getDisclosureType())
                .title(request.getTitle())
                .summary(request.getSummary())
                .fileUrl(fileUrl)
                .status(DisclosureStatus.PENDING)
                .stockNameSnapshot(base.getStockNameSnapshot())
                .tickerSnapshot(base.getTickerSnapshot())
                .originId(rootId)
                .revisionNo(nextRev)
                .build();
        return disclosureRepository.save(revision);
    }

    /** 추가공시 등록 (새로운 번호 체인 시작, previousId로 본공시 연결) */
    @Transactional
    public Disclosure additional(UUID baseId, DisclosureAdditionalReqFormDto request) {
        Disclosure base = disclosureRepository.findById(baseId)
                .orElseThrow(() -> new IllegalArgumentException("공시를 찾을 수 없습니다."));
        if (base.getStatus() != DisclosureStatus.APPROVED) {
            throw new IllegalStateException("승인된 공시만 추가공시를 등록할 수 있습니다.");
        }
        if (base.getRelationType() != DisclosureRelationType.NONE) {
            throw new IllegalStateException("추가공시는 본공시에만 허용됩니다.");
        }

        // 소유/상장 검증
        validateListedStockOwnership(base.getStockId());

        // 파일 업로드(pending)
        String type = base.getDisclosureType().name().toLowerCase();
        LocalDate today = LocalDate.now();
        String prefix = String.format("disclosures/pending/%s/%04d/%02d/%s",
                type, today.getYear(), today.getMonthValue(), base.getStockId());
        String fileUrl = uploadOrThrow(request.getFile(), prefix);

        Disclosure additional = Disclosure.builder()
                .stockId(base.getStockId())
                .disclosureType(base.getDisclosureType())
                .title(request.getTitle())
                .summary(request.getSummary())
                .fileUrl(fileUrl)
                .status(DisclosureStatus.PENDING)
                .stockNameSnapshot(base.getStockNameSnapshot())
                .tickerSnapshot(base.getTickerSnapshot())
                // originId는 새 체인 시작이므로 null 유지(승인 시 새 번호 부여)
                .relationType(DisclosureRelationType.ADDITIONAL)
                .previousId(base.getId())
                // revisionNo 기본 0 (builder default)
                .build();
        return disclosureRepository.save(additional);
    }

    private String uploadOrThrow(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("공시 파일이 비어 있습니다.");
        }
        return s3Manager.upload(file, prefix);
    }

    /**
     * 요청자의 기업 소속 + 종목 상장 여부 검증
     */
    private void validateListedStockOwnership(UUID stockId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomAdminPrincipal adminPrincipal)) {
            throw new IllegalArgumentException("인증되지 않은 요청입니다.");
        }
        if (adminPrincipal.role() != Role.CORPORATION) {
            throw new IllegalArgumentException("기업 관리자만 공시를 등록/수정할 수 있습니다.");
        }

        Admin admin = adminRepository.findById(adminPrincipal.id())
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));
        if (admin.getCorporation() == null) {
            throw new IllegalArgumentException("해당 관리자 계정에 연결된 기업이 없습니다.");
        }
        UUID corporationId = admin.getCorporation().getId();

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다."));
        if (!corporationId.equals(stock.getCorporationId())) {
            throw new IllegalArgumentException("해당 기업의 종목이 아닙니다.");
        }
        if (stock.getStatus() != Stock.Status.LISTED) {
            throw new IllegalStateException("상장된 종목에만 공시를 낼 수 있습니다.");
        }
    }
}
