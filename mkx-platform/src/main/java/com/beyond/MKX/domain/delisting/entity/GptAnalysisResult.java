package com.beyond.MKX.domain.delisting.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * GPT AI 분석 결과 기록 엔티티
 * 
 * 재무제표 데이터를 GPT API로 분석한 결과를 기록합니다.
 * 위반 여부와 관계없이 모든 분석 결과를 보존하여 추후 참조할 수 있도록 합니다.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "gpt_analysis_results",
       indexes = {
           @Index(name = "idx_gpt_analysis_stock_id", columnList = "stock_id"),
           @Index(name = "idx_gpt_analysis_criteria", columnList = "criteria_code"),
           @Index(name = "idx_gpt_analysis_date", columnList = "analysis_date"),
           @Index(name = "idx_gpt_analysis_type", columnList = "analysis_type")
       })
@Comment("GPT AI 분석 결과 기록")
public class GptAnalysisResult extends BaseIdAndTimeEntity {

    /**
     * 분석 대상 주식 ID
     */
    @Column(name = "stock_id", nullable = false)
    @Comment("분석 대상 주식 ID")
    private UUID stockId;

    /**
     * 분석 기준 코드
     */
    @Column(name = "criteria_code", nullable = false, length = 50)
    @Comment("분석 기준 코드")
    private String criteriaCode;

    /**
     * 분석 유형
     */
    @Column(name = "analysis_type", nullable = false, length = 30)
    @Comment("분석 유형")
    private String analysisType;

    /**
     * GPT 분석 위험도 점수 (0-10점)
     */
    @Column(name = "risk_score", precision = 3, scale = 1)
    @Comment("GPT 분석 위험도 점수")
    private BigDecimal riskScore;

    /**
     * GPT 분석 상세 설명
     */
    @Column(name = "analysis_description", columnDefinition = "TEXT")
    @Comment("GPT 분석 상세 설명")
    private String analysisDescription;

    /**
     * GPT 분석 판단 근거
     */
    @Column(name = "analysis_reasoning", columnDefinition = "TEXT")
    @Comment("GPT 분석 판단 근거")
    private String analysisReasoning;

    /**
     * 분석 시점
     */
    @Column(name = "analysis_date", nullable = false)
    @Comment("분석 시점")
    private LocalDateTime analysisDate;

    /**
     * 분석에 사용된 재무 데이터 (JSON 형태)
     */
    @Column(name = "financial_data", columnDefinition = "TEXT")
    @Comment("분석에 사용된 재무 데이터")
    private String financialData;

    /**
     * 분석 성공 여부
     */
    @Column(name = "is_successful")
    @Comment("분석 성공 여부")
    @Builder.Default
    private Boolean isSuccessful = true;

    /**
     * 오류 메시지 (분석 실패 시)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    @Comment("오류 메시지")
    private String errorMessage;

    /**
     * 분석 소요 시간 (밀리초)
     */
    @Column(name = "processing_time_ms")
    @Comment("분석 소요 시간")
    private Long processingTimeMs;

    /**
     * 동시성 제어(낙관적 락)
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /**
     * 분석 유형 Enum
     */
    public enum AnalysisType {
        AUDIT_OPINION_QUALIFIED("한정의견 위험도 분석"),
        AUDIT_OPINION_ADVERSE("부정의견 위험도 분석"),
        AUDIT_OPINION_DISCLAIMER("의견거절 위험도 분석"),
        FINANCIAL_HEALTH("재무건전성 종합 분석"),
        RISK_ASSESSMENT("위험도 종합 평가");

        private final String description;

        AnalysisType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
