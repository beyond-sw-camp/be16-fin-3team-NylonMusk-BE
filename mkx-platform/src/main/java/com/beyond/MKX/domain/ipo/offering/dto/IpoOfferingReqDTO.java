package com.beyond.MKX.domain.ipo.offering.dto;

import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingType;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IpoOfferingReqDTO {
    /* 공모 물량 */
    @NotNull
    @Positive
    private Long offerQuantity;
    /* 청약 단위 */
    @NotNull
    @Positive
    private Integer lotSize;

    /* 청약 시작 시간 */
    @NotNull
    private LocalDateTime subscriptionStart;
    /* 청약 마감 시간 */
    @NotNull
    private LocalDateTime subscriptionEnd;
    /* 배정일 */
    @NotNull
    private LocalDate allocationDate;
    /* 환불 기준일 */
    @NotNull
    private LocalDate refundDate;

    /* 희망 공모가 최솟값 */
    @NotNull
    @Positive
    private Long priceBandMin;
    /* 희망 공모가 최댓값 */
    @NotNull
    @Positive
    private Long priceBandMax;

    /* 증거금률 */
    @NotNull
    @DecimalMin("0.00") @DecimalMax("100.00")
    private BigDecimal depositRate;

    // 수요예측 시작 시간
    @Column(nullable = true)
    private LocalDateTime bookBuildingStart;

    // 수요예측 마감 시간
    @Column(nullable = true)
    private LocalDateTime bookBuildingEnd;

    private IpoOfferingType offeringType;

    private LocalDate recordDate; // Follow-on(상장 후 추가 공모) || Rights_Issue(증자)일 때만 필요

    @Builder.Default
    private boolean rightsIssue = false; // 유상증자 여부

//    /* 청약 경쟁률 */
//    // 사후 집계값이지만 DTO에 두면 업데이트용 엔드포인트에서 활용
//    @DecimalMin("0.00") @DecimalMax("100.00")
//    @Builder.Default
//    private BigDecimal competitionRatio = BigDecimal.ZERO;

//    /* 확정 공모가 */
//    @Positive
//    private Long offerPrice;

    /* 배정 방식 */
    // TODO: 계좌 생성 이후 진행 할 예정
    /* 잔여 주식 배분 */
    // TODO: 계좌 생성 이후 진행 할 예정
}
