package com.beyond.MKX.domain.delisting.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상장폐지 기준 정의 엔티티
 * 
 * 증권거래소의 상장폐지 요건을 시스템화하여 관리합니다.
 * 위키백과 상장폐지 요건을 기반으로 재무/거래/법규 기준을 체계적으로 정의합니다.
 *
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "delisting_criteria",
       indexes = {
           @Index(name = "idx_delisting_criteria_code", columnList = "criteria_code"),
           @Index(name = "idx_delisting_criteria_type", columnList = "criteria_type"),
           @Index(name = "idx_delisting_criteria_active", columnList = "is_active"),
           @Index(name = "idx_delisting_criteria_operator", columnList = "comparison_operator")
       })
@Comment("상장폐지 기준 정의")
public class DelistingCriteria extends BaseIdAndTimeEntity {

    /**
     * 기준 코드 (고유 식별자)
     * 시스템 내에서 기준을 식별하는 고유 코드 (예: LOW_REVENUE_2Y)
     */
    @Column(name = "criteria_code", nullable = false, unique = true, length = 50)
    @Comment("기준 코드")
    private String criteriaCode;

    /**
     * 기준명 (한글)
     * 사용자가 이해하기 쉬운 기준 설명 (예: "매출액 50억원 미만 2년 연속")
     */
    @Column(name = "criteria_name", nullable = false, length = 100)
    @Comment("기준명")
    private String criteriaName;

    /**
     * 기준 유형
     * FINANCIAL: 재무 기준, TRADING: 거래 기준, REGULATORY: 법규 기준
     */
    @Column(name = "criteria_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Comment("기준 유형")
    private CriteriaType criteriaType;

    /**
     * 기준값 (임계값)
     * 해당 기준의 임계값 (예: 50억원, 200%, 1000원 등)
     */
    @Column(name = "threshold_value", precision = 19, scale = 4)
    @Comment("기준값 (임계값)")
    private BigDecimal thresholdValue;

    /**
     * 비교 연산자
     * LESS_THAN: 미만, LESS_THAN_OR_EQUAL: 이하, GREATER_THAN: 초과, GREATER_THAN_OR_EQUAL: 이상, EQUAL: 동일, NOT_EQUAL: 상이
     */
    @Column(name = "comparison_operator", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Comment("비교 연산자")
    @Builder.Default
    private ComparisonOperator comparisonOperator = ComparisonOperator.LESS_THAN;

    /**
     * 연속 기간 (일/월/분기/년)
     * 기준 위반이 연속으로 지속되어야 하는 기간
     */
    @Column(name = "threshold_period")
    @Comment("연속 기간")
    private Integer thresholdPeriod;

    /**
     * 기준 단위
     * threshold_period의 단위 (DAYS, MONTHS, QUARTERS, YEARS)
     */
    @Column(name = "threshold_unit", length = 20)
    @Enumerated(EnumType.STRING)
    @Comment("기준 단위")
    private ThresholdUnit thresholdUnit;

    /**
     * 상세 설명
     * 기준에 대한 자세한 설명 및 계산 방법
     */
    @Column(name = "description", columnDefinition = "TEXT")
    @Comment("상세 설명")
    private String description;

    /**
     * 활성화 여부
     * 현재 해당 기준이 적용 중인지 여부
     */
    @Column(name = "is_active", nullable = false)
    @Comment("활성화 여부")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 기준 생성자 ID
     * 해당 기준을 생성한 관리자
     */
    @Column(name = "created_by")
    @Comment("기준 생성자 ID")
    private UUID createdBy;

    /**
     * 기준 수정자 ID
     * 해당 기준을 마지막으로 수정한 관리자
     */
    @Column(name = "updated_by")
    @Comment("기준 수정자 ID")
    private UUID updatedBy;

    /**
     * 기준 적용 시작일
     * 해당 기준이 적용되기 시작한 날짜
     */
    @Column(name = "effective_from")
    @Comment("기준 적용 시작일")
    private LocalDateTime effectiveFrom;

    /**
     * 기준 적용 종료일
     * 해당 기준이 적용이 종료되는 날짜 (null: 무제한)
     */
    @Column(name = "effective_to")
    @Comment("기준 적용 종료일")
    private LocalDateTime effectiveTo;

    /**
     * 동시성 제어(낙관적 락)
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /**
     * 기준 단위 Enum
     */
    public enum ThresholdUnit {
        DAYS("일"),
        MONTHS("월"),
        QUARTERS("분기"),
        YEARS("년");

        private final String description;

        ThresholdUnit(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 비교 연산자 Enum
     */
    public enum ComparisonOperator {
        LESS_THAN("미만"),
        LESS_THAN_OR_EQUAL("이하"),
        GREATER_THAN("초과"),
        GREATER_THAN_OR_EQUAL("이상"),
        EQUAL("동일"),
        NOT_EQUAL("상이");

        private final String description;

        ComparisonOperator(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
