package com.beyond.MKX.common.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${openai.max-tokens:1000}")
    private int maxTokens;

    @Value("${openai.temperature:0.3}")
    private double temperature;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    /**
     * 재무제표 데이터를 GPT API로 분석하여 감사 의견 위험도 평가
     */
    public AuditOpinionAnalysis analyzeAuditOpinionRisk(Map<String, Object> financialData) {
        try {
            String prompt = buildAuditAnalysisPrompt(financialData);
            String response = callGptApi(prompt);
            return parseAuditAnalysisResponse(response);
        } catch (Exception e) {
            log.error("GPT API 호출 실패", e);
            return AuditOpinionAnalysis.builder()
                    .riskScore(BigDecimal.ZERO)
                    .description("GPT API 호출 실패: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 감사 의견 분석을 위한 프롬프트 생성
     */
    private String buildAuditAnalysisPrompt(Map<String, Object> financialData) {
        // null이 아닌 데이터만 필터링하여 프롬프트에 포함
        StringBuilder dataSection = new StringBuilder();
        
        if (financialData.get("totalAssets") != null) {
            dataSection.append(String.format("- 총자산: %,d원\n", financialData.get("totalAssets")));
        }
        if (financialData.get("totalLiabilities") != null) {
            dataSection.append(String.format("- 총부채: %,d원\n", financialData.get("totalLiabilities")));
        }
        if (financialData.get("totalEquity") != null) {
            dataSection.append(String.format("- 자기자본: %,d원\n", financialData.get("totalEquity")));
        }
        if (financialData.get("revenue") != null) {
            dataSection.append(String.format("- 매출액: %,d원\n", financialData.get("revenue")));
        }
        if (financialData.get("operatingIncome") != null) {
            dataSection.append(String.format("- 영업이익: %,d원\n", financialData.get("operatingIncome")));
        }
        if (financialData.get("netIncome") != null) {
            dataSection.append(String.format("- 순이익: %,d원\n", financialData.get("netIncome")));
        }
        if (financialData.get("currentRatio") != null) {
            dataSection.append(String.format("- 유동비율: %.2f\n", financialData.get("currentRatio")));
        }
        if (financialData.get("debtRatio") != null) {
            dataSection.append(String.format("- 부채비율: %.2f%%\n", financialData.get("debtRatio")));
        }
        if (financialData.get("roe") != null) {
            dataSection.append(String.format("- ROE(자기자본이익률): %.2f%%\n", financialData.get("roe")));
        }
        if (financialData.get("roa") != null) {
            dataSection.append(String.format("- ROA(총자산이익률): %.2f%%\n", financialData.get("roa")));
        }
        
        return String.format("""
            다음 재무제표 데이터를 종합적으로 분석하여 감사 의견의 위험도를 평가해주세요:
            
            📊 재무 데이터:
            %s
            
            🎯 감사 의견 위험도 평가 기준:
            
            1. AUDIT_OPINION_QUALIFIED (한정의견):
               - 특정 사항에 대해 한정된 의견을 표명하는 경우
               - 재무제표의 일부 항목에 대해 불확실성이 있는 경우
               - 중요하지만 전체적으로는 적정한 경우
            
            2. AUDIT_OPINION_ADVERSE (부정의견):
               - 재무제표가 적정하게 작성되지 않았을 때
               - 중대한 왜곡이나 오류가 발견된 경우
               - 회계기준을 심각하게 위반한 경우
            
            3. AUDIT_OPINION_DISCLAIMER (의견거절):
               - 감사 증거를 충분히 확보할 수 없는 경우
               - 기업의 협조가 부족한 경우
               - 특별한 상황으로 인해 감사가 불가능한 경우
            
            📈 분석 시 고려사항:
            - 수익성 지표 (ROE, ROA, 영업이익률)
            - 안정성 지표 (부채비율, 자기자본비율)
            - 유동성 지표 (유동비율)
            - 성장성 지표 (매출액, 순이익 추이)
            - 재무구조의 건전성
            - 회계처리의 적정성
            
            ⚠️ 중요사항:
            - 제공된 데이터만을 기준으로 분석하세요
            - null 값이나 누락된 데이터는 언급하지 마세요
            - 실제 재무상태를 종합적으로 평가하세요
            - 구체적인 수치와 비율을 근거로 제시하세요
            
            📝 응답 형식:
            {
              "riskScore": 위험도점수(0-10),
              "description": "간결한 위험도 설명",
              "reasoning": "상세한 분석 근거와 판단 이유 (구체적인 수치와 비율 포함)"
            }
            """, dataSection.toString());
    }

    /**
     * GPT API 호출
     */
    private String callGptApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", new Object[]{message});

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                OPENAI_API_URL,
                HttpMethod.POST,
                request,
                String.class
        );

        return response.getBody();
    }

    /**
     * GPT 응답을 파싱하여 분석 결과 반환
     */
    private AuditOpinionAnalysis parseAuditAnalysisResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            String content = jsonNode.get("choices").get(0).get("message").get("content").asText();
            
            // JSON 응답 파싱 시도
            try {
                JsonNode analysisNode = objectMapper.readTree(content);
                BigDecimal riskScore = BigDecimal.valueOf(analysisNode.get("riskScore").asDouble());
                String description = analysisNode.get("description").asText();
                String reasoning = analysisNode.get("reasoning").asText();
                
                return AuditOpinionAnalysis.builder()
                        .riskScore(riskScore)
                        .description(description)
                        .reasoning(reasoning)
                        .build();
            } catch (Exception e) {
                // JSON 파싱 실패 시 텍스트에서 점수 추출
                BigDecimal riskScore = extractRiskScoreFromText(content);
                return AuditOpinionAnalysis.builder()
                        .riskScore(riskScore)
                        .description(content)
                        .reasoning("GPT 분석 결과")
                        .build();
            }
        } catch (Exception e) {
            log.error("GPT 응답 파싱 실패", e);
            return AuditOpinionAnalysis.builder()
                    .riskScore(BigDecimal.valueOf(5))
                    .description("GPT 응답 파싱 실패: " + e.getMessage())
                    .reasoning("기본값 사용")
                    .build();
        }
    }

    /**
     * 텍스트에서 위험도 점수 추출
     */
    private BigDecimal extractRiskScoreFromText(String text) {
        try {
            // "점수: X" 또는 "score: X" 패턴 찾기
            String[] lines = text.split("\n");
            for (String line : lines) {
                if (line.toLowerCase().contains("점수") || line.toLowerCase().contains("score")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        String scoreStr = parts[1].trim().replaceAll("[^0-9.]", "");
                        if (!scoreStr.isEmpty()) {
                            return BigDecimal.valueOf(Double.parseDouble(scoreStr));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("점수 추출 실패", e);
        }
        return BigDecimal.valueOf(5); // 기본값
    }

    /**
     * 감사 의견 분석 결과 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class AuditOpinionAnalysis {
        private BigDecimal riskScore;
        private String description;
        private String reasoning;
    }
}
