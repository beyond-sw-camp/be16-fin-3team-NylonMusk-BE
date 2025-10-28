package com.beyond.MKX.domain.delisting.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상장폐지 이력 추적 엔티티
 * 
 * 상장폐지 과정의 모든 단계와 상태 변화를 상세히 기록합니다.
 * 금융 규제 준수를 위한 완전한 감사 추적(Audit Trail)을 제공하며,
 * 상장폐지 절차의 투명성과 책임성을 보장합니다.
 *
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "delisting_history",
       indexes = {
           @Index(name = "idx_delisting_hist_stock_id", columnList = "stock_id"),
           @Index(name = "idx_delisting_hist_action_type", columnList = "action_type"),
           @Index(name = "idx_delisting_hist_execution_date", columnList = "execution_date"),
           @Index(name = "idx_delisting_hist_executed_by", columnList = "executed_by"),
           @Index(name = "idx_delisting_hist_stage_change", columnList = "from_stage, to_stage")
       })
@Comment("상장폐지 이력 추적")
public class DelistingHistory extends BaseIdAndTimeEntity {

    /**
     * 대상 주식 ID
     * 상장폐지 이력이 발생한 주식
     */
    @Column(name = "stock_id", nullable = false)
    @Comment("대상 주식 ID")
    private UUID stockId;

    /**
     * 액션 유형
     * CRITERIA_VIOLATION: 기준 위반, STAGE_CHANGE: 단계 변경,
     * DELISTING_NOTICE: 상장폐지 예고, DELISTING_EXECUTION: 상장폐지 실행,
     * DELISTING_CANCELLATION: 상장폐지 취소
     */
    @Column(name = "action_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Comment("액션 유형")
    private ActionType actionType;

    /**
     * 이전 단계
     * 상태 변경 전의 상장폐지 단계
     */
    @Column(name = "from_stage", length = 20)
    @Enumerated(EnumType.STRING)
    @Comment("이전 단계")
    private DelistingStage fromStage;

    /**
     * 변경 후 단계
     * 상태 변경 후의 상장폐지 단계
     */
    @Column(name = "to_stage", length = 20)
    @Enumerated(EnumType.STRING)
    @Comment("변경 후 단계")
    private DelistingStage toStage;

    /**
     * 액션 사유
     * 해당 액션이 발생한 구체적인 사유나 배경
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    @Comment("액션 사유")
    private String reason;

    /**
     * 관련 위반사항 ID 목록
     * JSON 형태로 관련된 위반사항들의 ID 저장
     */
    @Column(name = "violation_ids", columnDefinition = "TEXT")
    @Comment("관련 위반사항 ID 목록")
    private String violationIds;

    /**
     * 실행자 ID
     * 해당 액션을 실행한 관리자 또는 시스템
     */
    @Column(name = "executed_by")
    @Comment("실행자 ID")
    private UUID executedBy;

    /**
     * 실행일시
     * 액션이 실제로 실행된 시점
     */
    @Column(name = "execution_date", nullable = false)
    @Comment("실행일시")
    private LocalDateTime executionDate;

    /**
     * 실행 IP 주소
     * 액션 실행 시의 IP 주소 (보안 추적용)
     */
    @Column(name = "execution_ip", length = 45)
    @Comment("실행 IP 주소")
    private String executionIp;

    /**
     * 실행 세션 ID
     * 액션 실행 시의 세션 ID (추적용)
     */
    @Column(name = "session_id", length = 100)
    @Comment("실행 세션 ID")
    private String sessionId;

    /**
     * 실행 결과
     * SUCCESS: 성공, FAILED: 실패, PARTIAL: 부분 성공
     */
    @Column(name = "execution_result", length = 20)
    @Enumerated(EnumType.STRING)
    @Comment("실행 결과")
    @Builder.Default
    private ExecutionResult executionResult = ExecutionResult.SUCCESS;

    /**
     * 실행 메시지
     * 실행 결과에 대한 상세 메시지나 에러 내용
     */
    @Column(name = "execution_message", columnDefinition = "TEXT")
    @Comment("실행 메시지")
    private String executionMessage;

    /**
     * 영향받은 주주 수
     * 해당 액션으로 영향받은 주주의 수
     */
    @Column(name = "affected_shareholders")
    @Comment("영향받은 주주 수")
    private Integer affectedShareholders;

    /**
     * 영향받은 주식 수량
     * 해당 액션으로 영향받은 주식의 총 수량
     */
    @Column(name = "affected_shares")
    @Comment("영향받은 주식 수량")
    private Long affectedShares;

    /**
     * 영향받은 금액 (원)
     * 해당 액션으로 영향받은 금액의 총합
     */
    @Column(name = "affected_amount", precision = 19, scale = 2)
    @Comment("영향받은 금액 (원)")
    private java.math.BigDecimal affectedAmount;

    /**
     * 추가 메타데이터
     * 액션별 추가 정보를 JSON 형태로 저장
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    @Comment("추가 메타데이터")
    private String metadata;

    /**
     * 동시성 제어(낙관적 락)
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /**
     * 실행 결과 Enum
     */
    public enum ExecutionResult {
        SUCCESS("성공"),
        FAILED("실패"),
        PARTIAL("부분 성공"),
        CANCELLED("취소됨"),
        RETRY_ATTEMPTED("재처리 시도");

        private final String description;

        ExecutionResult(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
