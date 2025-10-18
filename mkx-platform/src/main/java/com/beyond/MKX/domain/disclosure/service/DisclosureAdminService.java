package com.beyond.MKX.domain.disclosure.service;

import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.common.s3.S3Manager;
import com.beyond.MKX.domain.disclosure.entity.*;
import com.beyond.MKX.domain.disclosure.repository.DisclosureDecisionRepository;
import com.beyond.MKX.domain.disclosure.repository.DisclosureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisclosureAdminService {

    private final DisclosureRepository disclosureRepository;
    private final DisclosureDecisionRepository decisionRepository;
    private final S3Manager s3Manager;

    @Transactional
    public Disclosure approve(UUID id) {
        Disclosure disclosure = disclosureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공시를 찾을 수 없습니다."));
        if (disclosure.getStatus() != DisclosureStatus.PENDING) {
            throw new IllegalStateException("대기(PENDING) 상태의 공시만 승인할 수 있습니다.");
        }
        // 상태 승인 + 공개 시각 세팅
        disclosure.approve();

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

        UUID adminId = currentAdminId();
        DisclosureDecision decision = DisclosureDecision.builder()
                .disclosureId(disclosure.getId())
                .adminId(adminId)
                .action(DisclosureDecisionAction.APPROVE)
                .build();
        decisionRepository.save(decision);
        return disclosure;
    }

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
