package com.beyond.MKX.domain.ipo.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpoOffering extends BaseIdAndTimeEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ipo_id")
    private Ipo ipo;
    /* 확정 공모가 */
    private Long offerPrice;
    /* 공모 물량 */
    private Long offerQuantity;
    /* 청약 단위 */
    private Long lotSize;

    /* 청약 시작 시간 */
    private LocalDateTime subscriptionStart;
    /* 청약 마감 시간 */
    private LocalDateTime subscriptionEnd;
    /* 배정일 */
    private LocalDate allocationDate;
    /* 환불 기준일 */
    private LocalDate refundDate;

    /* 희망 공모가 최솟값 */
    private Long priceBandMin;
    /* 희망 공모가 최댓값 */
    private Long priceBandMax;
    /* 증거금률 */
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal depositRate;
    /* 상태 */
    private IpoOfferingStatus ipoOfferingStatus;

    /* 잔여 주식 배분 */
    // TODO: 계좌 생성 이후 진행 할 예정

    /* 상한 비율 */
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal capRatio;
    /* 청약 경쟁률 */
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal competitionRatio;

    /* 배정 방식 */
    // TODO: 계좌 생성 이후 진행 할 예정
}
