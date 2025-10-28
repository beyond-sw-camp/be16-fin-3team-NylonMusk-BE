package com.beyond.MKX.domain.delisting.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 거래소 지원금 관리 엔티티
 * 
 * 상장폐지 시 거래소에서 지급한 지원금을 관리합니다.
 * 실제 금융 환경에서는 이런 지원금이 거래소의 채권으로 기록됩니다.
 * 
 */
@Entity
@Table(name = "exchange_support_fund", indexes = {
    @Index(name = "idx_exchange_support_stock", columnList = "stock_id"),
    @Index(name = "idx_exchange_support_corporation", columnList = "corporation_id"),
    @Index(name = "idx_exchange_support_status", columnList = "status"),
    @Index(name = "idx_exchange_support_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Comment("거래소 지원금 관리")
public class ExchangeSupportFund extends BaseIdAndTimeEntity {

    /**
     * 상장폐지된 주식 ID
     */
    @Column(name = "stock_id", nullable = false)
    @Comment("상장폐지된 주식 ID")
    private UUID stockId;

    /**
     * 기업 ID
     */
    @Column(name = "corporation_id", nullable = false)
    @Comment("기업 ID")
    private UUID corporationId;

    /**
     * 지원금 금액
     */
    @Column(name = "support_amount", nullable = false, precision = 19, scale = 2)
    @Comment("지원금 금액")
    private BigDecimal supportAmount;

    /**
     * 지원금 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "support_type", nullable = false, length = 20)
    @Comment("지원금 유형")
    private SupportType supportType;

    /**
     * 지원금 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    @Comment("지원금 상태")
    private SupportStatus status = SupportStatus.ACTIVE;

    /**
     * 지원 사유
     */
    @Column(name = "reason", length = 500)
    @Comment("지원 사유")
    private String reason;

    /**
     * 상환 예정일
     */
    @Column(name = "repayment_due_date")
    @Comment("상환 예정일")
    private LocalDateTime repaymentDueDate;

    /**
     * 상환 금액
     */
    @Column(name = "repaid_amount", precision = 19, scale = 2)
    @Builder.Default
    @Comment("상환 금액")
    private BigDecimal repaidAmount = BigDecimal.ZERO;

    /**
     * 이자율 (연%)
     */
    @Column(name = "interest_rate", precision = 5, scale = 2)
    @Builder.Default
    @Comment("이자율 (연%)")
    private BigDecimal interestRate = BigDecimal.ZERO;

    /**
     * 처리자 ID
     */
    @Column(name = "processed_by")
    @Comment("처리자 ID")
    private UUID processedBy;

    /**
     * 처리 일시
     */
    @Column(name = "processed_at")
    @Comment("처리 일시")
    private LocalDateTime processedAt;

    /**
     * 비고
     */
    @Column(name = "remarks", length = 1000)
    @Comment("비고")
    private String remarks;

    /**
     * 낙관적 락을 위한 버전 필드
     */
    @Version
    @Column(name = "version")
    @Comment("버전")
    private Long version;

    /**
     * 지원금 유형 Enum
     */
    public enum SupportType {
        COMPENSATION_LOAN("보상금 대출"),      // 상장폐지 보상금 지원
        OPERATING_LOAN("운영자금 대출"),      // 기업 운영자금 지원
        EMERGENCY_LOAN("긴급자금 대출"),      // 긴급 상황 지원
        GUARANTEE("보증"),                   // 보증 제공
        DIRECT_SUPPORT("직접 지원");         // 직접 지원금

        private final String description;

        SupportType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 지원금 상태 Enum
     */
    public enum SupportStatus {
        ACTIVE("활성"),           // 대출 중
        REPAID("상환완료"),       // 완전 상환
        PARTIAL_REPAID("부분상환"), // 부분 상환
        OVERDUE("연체"),         // 연체
        WRITTEN_OFF("대손처리"),  // 대손 처리
        CANCELLED("취소");       // 취소

        private final String description;

        SupportStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 상환 처리
     */
    public void repay(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("상환 금액은 0보다 커야 합니다");
        }
        
        BigDecimal newRepaidAmount = this.repaidAmount.add(amount);
        if (newRepaidAmount.compareTo(this.supportAmount) > 0) {
            throw new IllegalArgumentException("상환 금액이 지원금을 초과할 수 없습니다");
        }
        
        this.repaidAmount = newRepaidAmount;
        
        // 완전 상환 여부 확인
        if (this.repaidAmount.compareTo(this.supportAmount) >= 0) {
            this.status = SupportStatus.REPAID;
        } else {
            this.status = SupportStatus.PARTIAL_REPAID;
        }
    }

    /**
     * 연체 처리
     */
    public void markOverdue() {
        if (this.status == SupportStatus.ACTIVE && 
            this.repaymentDueDate != null && 
            LocalDateTime.now().isAfter(this.repaymentDueDate)) {
            this.status = SupportStatus.OVERDUE;
        }
    }

    /**
     * 대손 처리
     */
    public void writeOff() {
        this.status = SupportStatus.WRITTEN_OFF;
    }

    /**
     * 잔여 금액 계산
     */
    public BigDecimal getRemainingAmount() {
        return this.supportAmount.subtract(this.repaidAmount);
    }
}
