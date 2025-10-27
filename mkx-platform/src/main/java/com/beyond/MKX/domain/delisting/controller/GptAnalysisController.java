package com.beyond.MKX.domain.delisting.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.delisting.dto.GptAnalysisResultResDto;
import com.beyond.MKX.domain.delisting.dto.ComprehensiveAnalysisResDto;
import com.beyond.MKX.domain.delisting.entity.GptAnalysisResult;
import com.beyond.MKX.domain.delisting.service.GptAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/delisting/gpt-analysis")
@RequiredArgsConstructor
public class GptAnalysisController {

    private final GptAnalysisService gptAnalysisService;

    /**
     * 주식에 대한 GPT 분석 실행
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> performAnalysis(@RequestBody Map<String, Object> request) {
        UUID stockId = UUID.fromString((String) request.get("stockId"));
        String criteriaCode = (String) request.get("criteriaCode");
        String analysisType = (String) request.get("analysisType");
        @SuppressWarnings("unchecked")
        Map<String, Object> financialData = (Map<String, Object>) request.get("financialData");

        GptAnalysisResult result = gptAnalysisService.performAnalysis(stockId, criteriaCode, analysisType, financialData);
        GptAnalysisResultResDto resDto = mapToResDto(result);

        return ApiResponse.created(resDto, "GPT 분석 완료");
    }

    /**
     * 주식별 GPT 분석 결과 조회
     */
    @GetMapping("/stock/{stockId}")
    public ResponseEntity<?> getAnalysisResultsByStock(@PathVariable UUID stockId) {
        List<GptAnalysisResult> results = gptAnalysisService.getAnalysisResultsByStock(stockId);
        List<GptAnalysisResultResDto> resDtos = results.stream()
                .map(this::mapToResDto)
                .toList();

        return ApiResponse.ok(resDtos, "주식별 GPT 분석 결과 조회 성공");
    }

    /**
     * 주식별 특정 분석 유형의 결과 조회
     */
    @GetMapping("/stock/{stockId}/type/{analysisType}")
    public ResponseEntity<?> getAnalysisResultsByStockAndType(
            @PathVariable UUID stockId, 
            @PathVariable String analysisType) {
        List<GptAnalysisResult> results = gptAnalysisService.getAnalysisResultsByStockAndType(stockId, analysisType);
        List<GptAnalysisResultResDto> resDtos = results.stream()
                .map(this::mapToResDto)
                .toList();

        return ApiResponse.ok(resDtos, "주식별 특정 분석 유형 결과 조회 성공");
    }

    /**
     * 주식별 최근 분석 결과 조회
     */
    @GetMapping("/stock/{stockId}/latest")
    public ResponseEntity<?> getLatestAnalysisResult(@PathVariable UUID stockId) {
        GptAnalysisResult result = gptAnalysisService.getLatestAnalysisResult(stockId);
        
        if (result == null) {
            return ApiResponse.ok(null, "분석 결과가 없습니다.");
        }

        GptAnalysisResultResDto resDto = mapToResDto(result);
        return ApiResponse.ok(resDto, "최근 GPT 분석 결과 조회 성공");
    }

    /**
     * 주식별 최근 N일간의 분석 결과 조회
     */
    @GetMapping("/stock/{stockId}/recent/{days}")
    public ResponseEntity<?> getRecentAnalysisResults(
            @PathVariable UUID stockId, 
            @PathVariable int days) {
        List<GptAnalysisResult> results = gptAnalysisService.getRecentAnalysisResults(stockId, days);
        List<GptAnalysisResultResDto> resDtos = results.stream()
                .map(this::mapToResDto)
                .toList();

        return ApiResponse.ok(resDtos, String.format("최근 %d일간 GPT 분석 결과 조회 성공", days));
    }

    private GptAnalysisResultResDto mapToResDto(GptAnalysisResult result) {
        return GptAnalysisResultResDto.builder()
                .id(result.getId())
                .stockId(result.getStockId())
                .criteriaCode(result.getCriteriaCode())
                .analysisType(result.getAnalysisType())
                .riskScore(result.getRiskScore())
                .analysisDescription(result.getAnalysisDescription())
                .analysisReasoning(result.getAnalysisReasoning())
                .analysisDate(result.getAnalysisDate())
                .financialData(result.getFinancialData())
                .isSuccessful(result.getIsSuccessful())
                .errorMessage(result.getErrorMessage())
                .processingTimeMs(result.getProcessingTimeMs())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .version(result.getVersion())
                .build();
    }

    /**
     * 특정 주식의 모든 GPT 분석 결과를 종합적으로 조회 (분석 유형별 그룹화)
     */
    @GetMapping("/stock/{stockId}/comprehensive")
    public ResponseEntity<?> getComprehensiveAnalysisResults(@PathVariable UUID stockId) {
        try {
            // 모든 분석 결과 조회
            List<GptAnalysisResult> allResults = gptAnalysisService.getAnalysisResultsByStock(stockId);
            
            // 분석 유형별로 그룹화
            Map<String, List<GptAnalysisResultResDto>> groupedResults = allResults.stream()
                    .map(this::mapToResDto)
                    .collect(Collectors.groupingBy(GptAnalysisResultResDto::analysisType));
            
            // 각 분석 유형별 최신 결과만 추출
            Map<String, GptAnalysisResultResDto> latestByType = new HashMap<>();
            groupedResults.forEach((analysisType, results) -> {
                GptAnalysisResultResDto latest = results.stream()
                        .max(Comparator.comparing(GptAnalysisResultResDto::analysisDate))
                        .orElse(null);
                if (latest != null) {
                    latestByType.put(analysisType, latest);
                }
            });
            
            // 종합 분석 결과 생성
            ComprehensiveAnalysisResDto comprehensiveResult = ComprehensiveAnalysisResDto.builder()
                    .stockId(stockId)
                    .totalAnalysisCount(allResults.size())
                    .analysisTypes(latestByType.keySet().stream().toList())
                    .latestResultsByType(latestByType)
                    .allResults(allResults.stream().map(this::mapToResDto).toList())
                    .overallRiskScore(calculateOverallRiskScore(allResults))
                    .riskLevel(determineRiskLevel(calculateOverallRiskScore(allResults)))
                    .lastAnalysisDate(allResults.stream()
                            .map(GptAnalysisResult::getAnalysisDate)
                            .max(LocalDateTime::compareTo)
                            .orElse(null))
                    .build();
            
            return ApiResponse.ok(comprehensiveResult, "종합 GPT 분석 결과 조회 성공");
            
        } catch (Exception e) {
            return ApiResponse.ok(null, "종합 분석 결과 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 전체 위험도 점수 계산
     */
    private BigDecimal calculateOverallRiskScore(List<GptAnalysisResult> results) {
        return results.stream()
                .filter(r -> r.getRiskScore() != null)
                .map(GptAnalysisResult::getRiskScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 위험도 레벨 결정
     */
    private String determineRiskLevel(BigDecimal riskScore) {
        if (riskScore.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return "CRITICAL";
        } else if (riskScore.compareTo(BigDecimal.valueOf(15)) >= 0) {
            return "HIGH";
        } else if (riskScore.compareTo(BigDecimal.valueOf(10)) >= 0) {
            return "MEDIUM";
        } else if (riskScore.compareTo(BigDecimal.valueOf(5)) >= 0) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
}
