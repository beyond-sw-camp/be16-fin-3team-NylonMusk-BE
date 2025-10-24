package com.beyond.MKX.domain.delisting.service;

import com.beyond.MKX.common.openai.OpenAiService;
import com.beyond.MKX.domain.delisting.entity.GptAnalysisResult;
import com.beyond.MKX.domain.delisting.repository.GptAnalysisResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GptAnalysisService {

    private final OpenAiService openAiService;
    private final GptAnalysisResultRepository gptAnalysisResultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 주식에 대한 GPT 분석 실행 및 결과 저장
     */
    public GptAnalysisResult performAnalysis(UUID stockId, String criteriaCode, 
                                           String analysisType, Map<String, Object> financialData) {
        log.info("GPT 분석 시작: stockId={}, criteriaCode={}, analysisType={}", 
                stockId, criteriaCode, analysisType);

        long startTime = System.currentTimeMillis();
        LocalDateTime analysisDate = LocalDateTime.now();

        try {
            // GPT API 호출
            OpenAiService.AuditOpinionAnalysis analysis = openAiService.analyzeAuditOpinionRisk(financialData);
            
            long processingTime = System.currentTimeMillis() - startTime;

            // 분석 결과 저장
            GptAnalysisResult result = GptAnalysisResult.builder()
                    .stockId(stockId)
                    .criteriaCode(criteriaCode)
                    .analysisType(analysisType)
                    .riskScore(analysis.getRiskScore())
                    .analysisDescription(analysis.getDescription())
                    .analysisReasoning(analysis.getReasoning())
                    .analysisDate(analysisDate)
                    .financialData(convertFinancialDataToJson(financialData))
                    .isSuccessful(true)
                    .processingTimeMs(processingTime)
                    .build();

            GptAnalysisResult saved = gptAnalysisResultRepository.save(result);

            log.info("GPT 분석 완료: analysisId={}, riskScore={}, processingTime={}ms", 
                    saved.getId(), analysis.getRiskScore(), processingTime);

            return saved;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.error("GPT 분석 실패: stockId={}, criteriaCode={}", stockId, criteriaCode, e);

            // 실패한 분석도 기록
            GptAnalysisResult failedResult = GptAnalysisResult.builder()
                    .stockId(stockId)
                    .criteriaCode(criteriaCode)
                    .analysisType(analysisType)
                    .analysisDate(analysisDate)
                    .financialData(convertFinancialDataToJson(financialData))
                    .isSuccessful(false)
                    .errorMessage(e.getMessage())
                    .processingTimeMs(processingTime)
                    .build();

            return gptAnalysisResultRepository.save(failedResult);
        }
    }

    /**
     * 주식별 GPT 분석 결과 조회
     */
    @Transactional(readOnly = true)
    public List<GptAnalysisResult> getAnalysisResultsByStock(UUID stockId) {
        return gptAnalysisResultRepository.findByStockIdOrderByAnalysisDateDesc(stockId);
    }

    /**
     * 주식별 특정 분석 유형의 결과 조회
     */
    @Transactional(readOnly = true)
    public List<GptAnalysisResult> getAnalysisResultsByStockAndType(UUID stockId, String analysisType) {
        return gptAnalysisResultRepository.findByStockIdAndAnalysisTypeOrderByAnalysisDateDesc(stockId, analysisType);
    }

    /**
     * 주식별 최근 분석 결과 조회
     */
    @Transactional(readOnly = true)
    public GptAnalysisResult getLatestAnalysisResult(UUID stockId) {
        return gptAnalysisResultRepository.findLatestByStockId(stockId);
    }

    /**
     * 주식별 최근 N일간의 분석 결과 조회
     */
    @Transactional(readOnly = true)
    public List<GptAnalysisResult> getRecentAnalysisResults(UUID stockId, int days) {
        LocalDateTime sinceDate = LocalDateTime.now().minusDays(days);
        return gptAnalysisResultRepository.findByStockIdAndAnalysisDateAfterOrderByAnalysisDateDesc(stockId, sinceDate);
    }

    /**
     * 재무 데이터를 JSON 문자열로 변환
     */
    private String convertFinancialDataToJson(Map<String, Object> financialData) {
        try {
            return objectMapper.writeValueAsString(financialData);
        } catch (JsonProcessingException e) {
            log.warn("재무 데이터 JSON 변환 실패", e);
            return "{}";
        }
    }
}
