package com.beyond.MKX.domain.delisting.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.delisting.dto.DelistingViolationCreateReqDto;
import com.beyond.MKX.domain.delisting.dto.DelistingViolationResDto;
import com.beyond.MKX.domain.delisting.dto.DelistingProgressResDto;
import com.beyond.MKX.domain.delisting.entity.DelistingViolation;
import com.beyond.MKX.domain.delisting.repository.DelistingViolationRepository;
import com.beyond.MKX.domain.delisting.service.DelistingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/delisting")
@RequiredArgsConstructor
public class DelistingController {

    private final DelistingService delistingService;
    private final DelistingViolationRepository violationRepo;

    /**
     * 기준 위반 감지 및 기록
     */
    @PostMapping("/violations/detect")
    public ResponseEntity<?> detectViolation(@Valid @RequestBody DelistingViolationCreateReqDto req) {
        DelistingViolation violation = delistingService.detectViolation(
                req.stockId(), 
                req.criteriaCode(), 
                req.currentValue(), 
                req.description()
        );
        
        if (violation == null) {
            return ApiResponse.ok(null, "기준 위반이 아닙니다.");
        }
        
        DelistingViolationResDto res = mapToResDto(violation);
        return ApiResponse.created(res, "기준 위반 감지 및 기록 완료");
    }

    /**
     * 상장폐지 진행 상황 종합 체크
     */
    @PostMapping("/progress/check/{stockId}")
    public ResponseEntity<?> checkDelistingProgress(@PathVariable UUID stockId) {
        DelistingProgressResDto result = delistingService.checkDelistingProgress(stockId);
        return ApiResponse.ok(result, "상장폐지 진행 상황 체크 완료");
    }

    /**
     * 위반 해결 처리
     */
    @PostMapping("/violations/{violationId}/resolve")
    public ResponseEntity<?> resolveViolation(@PathVariable UUID violationId,
                                            @RequestParam UUID resolvedBy,
                                            @RequestParam String description) {
        delistingService.resolveViolation(violationId, resolvedBy, description);
        return ApiResponse.ok(null, "위반 해결 처리 완료");
    }

    /**
     * 주식별 위반 목록 조회
     */
    @GetMapping("/violations/stock/{stockId}")
    public ResponseEntity<?> getViolationsByStock(@PathVariable UUID stockId) {
        List<DelistingViolation> violations = violationRepo.findByStockId(stockId);
        List<DelistingViolationResDto> res = violations.stream()
                .map(this::mapToResDto)
                .toList();
        return ApiResponse.ok(res, "주식별 위반 목록 조회 성공");
    }

    /**
     * 해결되지 않은 위반 목록 조회
     */
    @GetMapping("/violations/unresolved")
    public ResponseEntity<?> getUnresolvedViolations() {
        List<DelistingViolation> violations = violationRepo.findUnresolvedViolations();
        List<DelistingViolationResDto> res = violations.stream()
                .map(this::mapToResDto)
                .toList();
        return ApiResponse.ok(res, "해결되지 않은 위반 목록 조회 성공");
    }

    /**
     * 실패한 보상금 재처리
     */
    @PostMapping("/compensations/retry/{stockId}")
    public ResponseEntity<?> retryFailedCompensations(@PathVariable UUID stockId) {
        delistingService.retryFailedCompensations(stockId);
        return ApiResponse.ok(null, "보상금 재처리 완료");
    }

    /**
     * 상장폐지 실행
     */
    @PostMapping("/execute/{stockId}")
    public ResponseEntity<?> executeDelisting(@PathVariable UUID stockId) {
        delistingService.executeDelisting(stockId);
        return ApiResponse.ok(null, "상장폐지 실행 완료");
    }

    /**
     * 상장폐지 예고 발행
     */
    @PostMapping("/notice/{stockId}")
    public ResponseEntity<?> issueDelistingNotice(@PathVariable UUID stockId) {
        delistingService.issueDelistingNotice(stockId);
        return ApiResponse.ok(null, "상장폐지 예고 발행 완료");
    }

    /**
     * 상장폐지 절차 시작
     */
    @PostMapping("/process/start/{stockId}")
    public ResponseEntity<?> startDelistingProcess(@PathVariable UUID stockId) {
        delistingService.startDelistingProcess(stockId);
        return ApiResponse.ok(null, "상장폐지 절차 시작 완료");
    }

    /**
     * 재무제표 기반 자동 상장폐지 위험 감지
     */
    @PostMapping("/auto-detect/{stockId}")
    public ResponseEntity<?> detectViolationFromFinancials(@PathVariable UUID stockId) {
        delistingService.detectViolationFromFinancials(stockId);
        return ApiResponse.ok(null, "재무제표 기반 자동 위반 감지 완료");
    }

    /**
     * 상장폐지 위험 분석 리포트 생성
     */
    @GetMapping("/risk-report/{stockId}")
    public ResponseEntity<?> generateDelistingRiskReport(@PathVariable UUID stockId) {
        Map<String, Object> report = delistingService.generateDelistingRiskReport(stockId);
        return ApiResponse.ok(report, "상장폐지 위험 분석 리포트 생성 완료");
    }

    /**
     * 위험 해소 처리 (문제 없음 처리)
     */
    @PostMapping("/risk/resolve/{stockId}")
    public ResponseEntity<?> resolveRisk(@PathVariable UUID stockId,
                                        @RequestParam UUID adminId,
                                        @RequestParam(required = false) String reason) {
        delistingService.resolveRisk(stockId, adminId, reason);
        return ApiResponse.ok(null, "위험 해소 처리 완료");
    }

    private DelistingViolationResDto mapToResDto(DelistingViolation violation) {
        return DelistingViolationResDto.builder()
                .id(violation.getId())
                .stockId(violation.getStockId())
                .criteriaId(violation.getCriteriaId())
                .criteriaCode(violation.getCriteriaCode())
                .violationType(violation.getViolationType())
                .currentValue(violation.getCurrentValue())
                .thresholdValue(violation.getThresholdValue())
                .consecutivePeriods(violation.getConsecutivePeriods())
                .violationDate(violation.getViolationDate())
                .isResolved(violation.getIsResolved())
                .resolvedDate(violation.getResolvedDate())
                .resolvedBy(violation.getResolvedBy())
                .description(violation.getDescription())
                .severityScore(violation.getSeverityScore())
                .detectionMethod(violation.getDetectionMethod())
                .requiresAction(violation.getRequiresAction())
                .gptRiskScore(violation.getGptRiskScore())
                .gptAnalysisDescription(violation.getGptAnalysisDescription())
                .gptAnalysisReasoning(violation.getGptAnalysisReasoning())
                .gptAnalysisUsed(violation.getGptAnalysisUsed())
                .createdAt(violation.getCreatedAt())
                .updatedAt(violation.getUpdatedAt())
                .deletedAt(violation.getDeletedAt())
                .version(violation.getVersion())
                .build();
    }
}
