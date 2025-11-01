package com.beyond.MKX.domain.disclosure.service;

import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.common.s3.S3Manager;
import com.beyond.MKX.domain.financial.service.FinancialUploadService;
import com.beyond.MKX.domain.disclosure.entity.*;
import com.beyond.MKX.domain.disclosure.repository.DisclosureDecisionRepository;
import com.beyond.MKX.domain.disclosure.repository.DisclosureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisclosureAdminService {

    private final DisclosureRepository disclosureRepository;
    private final DisclosureDecisionRepository decisionRepository;
    private final S3Manager s3Manager;
    private final DisclosureNumberService numberService;
    private final FinancialUploadService financialUploadService;
    private final com.beyond.MKX.domain.delisting.service.DelistingService delistingService;

    /**
     * 공시 상세 조회 (관리자용)
     */
    public Disclosure getById(UUID id) {
        return disclosureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공시를 찾을 수 없습니다."));
    }

    @Transactional
    public Disclosure approve(UUID id) {
        Disclosure disclosure = disclosureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공시를 찾을 수 없습니다."));
        if (disclosure.getStatus() != DisclosureStatus.PENDING) {
            throw new IllegalStateException("대기(PENDING) 상태의 공시만 승인할 수 있습니다.");
        }

        // EARNINGS 공시는 파일 이동 전에 파싱/저장 우선 수행
        if (disclosure.getDisclosureType() == DisclosureType.EARNINGS) {
            financialUploadService.uploadFromDisclosure(disclosure);
        }

        // 상태 승인 + 공개 시각 세팅
        disclosure.approve();

        // displayNo 발급/상속 + 최신 토글
        UUID rootId = disclosure.getOriginId() != null ? disclosure.getOriginId() : disclosure.getId();
        if (disclosure.getOriginId() == null) {
            // 원본 승인 → 번호 발급(연도별 시퀀스)
            if (disclosure.getDisplayNo() == null || disclosure.getDisplayNo().isBlank()) {
                String disp = numberService.issueNumber();
                disclosure.setDisplayNo(disp);
            }
        } else {
            // 정정 승인 → 그룹 번호 상속 (여러 값이 있을 수 있어 첫 값 사용)
            List<String> numbers = disclosureRepository.findGroupDisplayNo(rootId);
            if (!numbers.isEmpty()) {
                disclosure.setDisplayNo(numbers.get(0));
            }
        }
        // 최신 플래그 갱신
        disclosureRepository.clearLatestByGroup(rootId);
        disclosure.setIsLatest(true);

        // 파일 경로를 approved prefix로 이동(copy + delete)
        String oldUrl = disclosure.getFileUrl();
        String type = disclosure.getDisclosureType().name().toLowerCase();
        int year = disclosure.getPublishedAt().getYear();
        int month = disclosure.getPublishedAt().getMonthValue();
        String destPrefix = String.format("disclosures/approved/%s/%04d/%02d/%s",
                type, year, month, disclosure.getStockId());

        String newUrl = s3Manager.copy(oldUrl, destPrefix);
        disclosure.updateFileUrl(newUrl);
        try { s3Manager.delete(oldUrl); } catch (Exception ignored) {}

        // 반드시 저장/플러시해서 isLatest, displayNo, fileUrl 반영 보장
        disclosureRepository.saveAndFlush(disclosure);

        UUID adminId = currentAdminId();
        DisclosureDecision decision = DisclosureDecision.builder()
                .disclosureId(disclosure.getId())
                .adminId(adminId)
                .action(DisclosureDecisionAction.APPROVE)
                .build();
        decisionRepository.save(decision);

        // 승인 후: 최신 공시 기반으로 상장폐지 조건 재검사 및 정상화 처리
        try {
            if (disclosure.getStockId() != null) {
                delistingService.onDisclosureApproved(disclosure.getStockId(), adminId);
            }
        } catch (Exception e) {
            // 재평가 실패는 승인 자체를 롤백하지 않음
            System.out.println("[Disclosure] 공시 승인 후 재평가 중 오류: disclosureId=" + id + ", stockId=" + disclosure.getStockId() + ", err=" + e.getMessage());
        }
        return disclosure;
    }

    public FileDownload downloadFile(UUID id) {
        Disclosure disclosure = disclosureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공시를 찾을 수 없습니다."));
        String url = disclosure.getFileUrl();
        byte[] bytes = s3Manager.download(url);
        String filename = extractFilename(url);
        String contentType = guessContentType(filename);
        return new FileDownload(bytes, filename, contentType);
    }

    private String extractFilename(String url) {
        if (url == null) return "disclosure-file";
        int idx = url.lastIndexOf('/');
        return (idx >= 0 && idx + 1 < url.length()) ? url.substring(idx + 1) : url;
    }

    private String guessContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    public record FileDownload(byte[] bytes, String filename, String contentType) {}

    @Transactional
    public Disclosure reject(UUID id, DisclosureRejectCode code, String reason) {
        Disclosure disclosure = disclosureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공시를 찾을 수 없습니다."));
        if (disclosure.getStatus() != DisclosureStatus.PENDING) {
            throw new IllegalStateException("대기(PENDING) 상태의 공시만 반려할 수 있습니다.");
        }
        String finalReason = reason;
        if (code != null && code != DisclosureRejectCode.OTHER) {
            if (finalReason == null || finalReason.isBlank()) {
                finalReason = defaultReason(code);
            }
        }

        disclosure.reject(code, finalReason);

        // 파일을 rejected prefix로 이동(copy + delete)
        String oldUrl = disclosure.getFileUrl();
        String type = disclosure.getDisclosureType().name().toLowerCase();
        LocalDate today = LocalDate.now();
        String destPrefix = String.format("disclosures/rejected/%s/%04d/%02d/%s",
                type, today.getYear(), today.getMonthValue(), disclosure.getStockId());

        String newUrl = s3Manager.copy(oldUrl, destPrefix);
        disclosure.updateFileUrl(newUrl);
        try { s3Manager.delete(oldUrl); } catch (Exception ignored) {}

        // 반려 후 경로 변경도 즉시 반영
        disclosureRepository.saveAndFlush(disclosure);

        UUID adminId = currentAdminId();
        DisclosureDecision decision = DisclosureDecision.builder()
                .disclosureId(disclosure.getId())
                .adminId(adminId)
                .action(DisclosureDecisionAction.REJECT)
                .rejectCode(code)
                .reason(finalReason)
                .build();
        decisionRepository.save(decision);
        return disclosure;
    }

    private String defaultReason(DisclosureRejectCode code) {
        return switch (code) {
            case FORMAT_ERROR -> "문서 형식 오류";
            case MISSING_INFO -> "필수 정보 누락";
            case WRONG_STOCK -> "종목 정보 불일치";
            case DUPLICATE -> "중복 제출";
            case POLICY_VIOLATION -> "게시 정책 위반";
            case OTHER -> "기타 사유";
        };
    }

    private UUID currentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomAdminPrincipal principal) {
            return principal.id();
        }
        throw new IllegalArgumentException("인증되지 않은 요청입니다.");
    }
}
