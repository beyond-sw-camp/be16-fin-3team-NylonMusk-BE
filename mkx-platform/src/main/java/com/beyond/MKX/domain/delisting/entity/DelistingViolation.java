package com.beyond.MKX.domain.delisting.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상장폐지 기준 위반 기록 엔티티
 * 
 * 각 주식이 상장폐지 기준을 위반한 내역을 상세히 기록합니다.
 * 위반 발생 시점, 위반 내용, 해결 여부 등을 추적하여 상장폐지 절차의 투명성을 보장합니다.
 *
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "delisting_violations",
       indexes = {
           @Index(name = "idx_delisting_viol_stock_id", columnList = "stock_id"),
           @Index(name = "idx_delisting_viol_criteria", columnList = "criteria_id"),
           @Index(name = "idx_delisting_viol_type", columnList = "violation_type"),
           @Index(name = "idx_delisting_viol_date", columnList = "violation_date"),
           @Index(name = "idx_delisting_viol_resolved", columnList = "is_resolved")
       })
@Comment("상장폐지 기준 위반 기록")
public class DelistingViolation extends BaseIdAndTimeEntity {

    /**
     * 위반한 주식 ID
     * 어떤 주식이 기준을 위반했는지 식별
     */
    @Column(name = "stock_id", nullable = false)
    @Comment("위반 주식 ID")
    private UUID stockId;

    /**
     * 위반한 기준 ID
     * 어떤 기준을 위반했는지 식별
     */
    @Column(name = "criteria_id", nullable = false)
    @Comment("위반 기준 ID")
    private UUID criteriaId;

    /**
     * 위반한 기준 코드
     * 기준 코드로 빠른 식별 가능 (예: LOW_REVENUE_2Y)
     */
    @Column(name = "criteria_code", nullable = false, length = 50)
    @Comment("위반 기준 코드")
    private String criteriaCode;

    /**
     * 위반 유형
     * WARNING: 경고 (개선 기회), CRITICAL: 위험 (상장폐지 임박)
     */
    @Column(name = "violation_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Comment("위반 유형")
    private ViolationType violationType;

    /**
     * 현재 측정값
     * 기준 위반 시점의 실제 측정값 (예: 현재 매출액, 부채비율 등)
     */
    @Column(name = "current_value", precision = 19, scale = 4)
    @Comment("현재 측정값")
    private BigDecimal currentValue;

    /**
     * 기준 임계값
     * 해당 기준의 임계값 (예: 50억원, 200% 등)
     */
    @Column(name = "threshold_value", precision = 19, scale = 4)
    @Comment("기준 임계값")
    private BigDecimal thresholdValue;

    /**
     * 연속 위반 기간
     * 해당 기준을 연속으로 위반한 기간 수
     */
    @Column(name = "consecutive_periods")
    @Comment("연속 위반 기간")
    @Builder.Default
    private Integer consecutivePeriods = 1;

    /**
     * 위반 발생일시
     * 기준 위반이 감지된 정확한 시점
     */
    @Column(name = "violation_date", nullable = false)
    @Comment("위반 발생일시")
    private LocalDateTime violationDate;

    /**
     * 위반 해결 여부
     * 해당 위반이 해결되었는지 여부
     */
    @Column(name = "is_resolved", nullable = false)
    @Comment("위반 해결 여부")
    @Builder.Default
    private Boolean isResolved = false;

    /**
     * 위반 해결일시
     * 위반이 해결된 시점 (null: 미해결)
     */
    @Column(name = "resolved_date")
    @Comment("위반 해결일시")
    private LocalDateTime resolvedDate;

    /**
     * 위반 해결자 ID
     * 위반 해결을 담당한 관리자
     */
    @Column(name = "resolved_by")
    @Comment("위반 해결자 ID")
    private UUID resolvedBy;

    /**
     * 위반 내용 상세 설명
     * 구체적인 위반 내용과 상황 설명
     */
    @Column(name = "description", columnDefinition = "TEXT")
    @Comment("위반 내용 상세")
    private String description;

    /**
     * 위반 심각도 점수
     * 위반의 심각도를 수치화한 점수 (1-10, 높을수록 심각)
     */
    @Column(name = "severity_score")
    @Comment("위반 심각도 점수")
    private Integer severityScore;

    /**
     * 위반 감지 방법
     * 자동 감지, 수동 감지, 외부 신고 등
     */
    @Column(name = "detection_method", length = 20)
    @Enumerated(EnumType.STRING)
    @Comment("위반 감지 방법")
    private DetectionMethod detectionMethod;

    /**
     * 추가 조치 필요 여부
     * 해당 위반에 대한 추가 조치가 필요한지 여부
     */
    @Column(name = "requires_action")
    @Comment("추가 조치 필요 여부")
    @Builder.Default
    private Boolean requiresAction = true;

    /**
     * GPT AI 분석 위험도 점수
     * GPT API로 분석한 위험도 점수 (0-10점)
     */
    @Column(name = "gpt_risk_score", precision = 3, scale = 1)
    @Comment("GPT AI 분석 위험도 점수")
    private BigDecimal gptRiskScore;

    /**
     * GPT AI 분석 상세 설명
     * GPT API가 제공한 상세한 분석 설명
     */
    @Column(name = "gpt_analysis_description", columnDefinition = "TEXT")
    @Comment("GPT AI 분석 상세 설명")
    private String gptAnalysisDescription;

    /**
     * GPT AI 분석 판단 근거
     * GPT API가 제공한 판단 근거 및 이유
     */
    @Column(name = "gpt_analysis_reasoning", columnDefinition = "TEXT")
    @Comment("GPT AI 분석 판단 근거")
    private String gptAnalysisReasoning;

    /**
     * GPT AI 분석 사용 여부
     * 해당 위반이 GPT AI 분석을 통해 감지되었는지 여부
     */
    @Column(name = "gpt_analysis_used")
    @Comment("GPT AI 분석 사용 여부")
    @Builder.Default
    private Boolean gptAnalysisUsed = false;

    /**
     * 동시성 제어(낙관적 락)
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /**
     * 위반 감지 방법 Enum
     */
    public enum DetectionMethod {
        AUTOMATIC("자동 감지"),
        MANUAL("수동 감지"),
        EXTERNAL_REPORT("외부 신고"),
        AUDIT("감사 발견"),
        REGULATORY_NOTICE("규제 당국 통보");

        private final String description;

        DetectionMethod(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
