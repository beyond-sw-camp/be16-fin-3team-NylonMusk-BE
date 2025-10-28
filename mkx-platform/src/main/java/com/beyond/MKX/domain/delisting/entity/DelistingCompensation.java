package com.beyond.MKX.domain.delisting.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상장폐지 보상 기록 엔티티
 * 
 * 상장폐지로 인해 주주에게 지급되는 보상금의 처리 내역을 관리합니다.
 * 금융 거래의 투명성과 추적성을 보장하기 위해 모든 보상 과정을 기록합니다.
 *
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "delisting_compensations", 
       indexes = {
           @Index(name = "idx_delisting_comp_stock_member", columnList = "stock_id, member_account_id"),
           @Index(name = "idx_delisting_comp_status", columnList = "status"),
           @Index(name = "idx_delisting_comp_requested_at", columnList = "requested_at")
       })
@Comment("상장폐지 보상 기록")
public class DelistingCompensation extends BaseIdAndTimeEntity {

    /**
     * 상장폐지된 주식 ID
     * 어떤 주식에 대한 보상인지 식별
     */
    @Column(name = "stock_id", nullable = false)
    @Comment("상장폐지 주식 ID")
    private UUID stockId;

    /**
     * 보상받을 회원 계좌 ID
     * 실제 보상금이 입금될 계좌
     */
    @Column(name = "member_account_id", nullable = false)
    @Comment("회원 계좌 ID")
    private UUID memberAccountId;

    /**
     * 보상금 총액 (원)
     * 정밀한 금융 계산을 위해 BigDecimal 사용
     */
    @Column(name = "compensation_amount", nullable = false, precision = 19, scale = 2)
    @Comment("보상금 총액 (원)")
    private BigDecimal compensationAmount;

    /**
     * 보상받은 주식 수량
     * 보상 대상이 된 주식의 개수
     */
    @Column(name = "stock_quantity", nullable = false)
    @Comment("보상받은 주식 수량")
    private Long stockQuantity;

    /**
     * 보상 기준 주가 (원)
     * 상장폐지 예고일 기준 주가로 보상금 계산
     */
    @Column(name = "compensation_price", nullable = false, precision = 19, scale = 2)
    @Comment("보상 기준 주가 (원)")
    private BigDecimal compensationPrice;

    /**
     * 보상 요청일시
     * 상장폐지 예고 시점에 보상 요청이 생성된 시점
     */
    @Column(name = "requested_at", nullable = false)
    @Comment("보상 요청일시")
    private LocalDateTime requestedAt;

    /**
     * 보상 처리 완료일시
     * 실제 보상금이 지급된 시점 (null: 미처리)
     */
    @Column(name = "processed_at")
    @Comment("보상 처리 완료일시")
    private LocalDateTime processedAt;

    /**
     * 보상 처리 담당자 ID
     * 보상금 지급을 승인/처리한 관리자
     */
    @Column(name = "processed_by")
    @Comment("보상 처리 담당자 ID")
    private UUID processedBy;

    /**
     * 보상 처리 상태
     * PENDING: 대기, PROCESSING: 처리중, COMPLETED: 완료, FAILED: 실패
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Comment("보상 처리 상태")
    private CompensationStatus status = CompensationStatus.PENDING;

    /**
     * 실패 사유 (상태가 FAILED인 경우)
     * 보상 처리 실패 시 구체적인 실패 원인
     */
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    @Comment("실패 사유")
    private String failureReason;

    /**
     * 보상 처리 메모
     * 추가적인 처리 내역이나 특이사항 기록
     */
    @Column(name = "processing_memo", columnDefinition = "TEXT")
    @Comment("보상 처리 메모")
    private String processingMemo;

    /**
     * 동시성 제어(낙관적 락)
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
