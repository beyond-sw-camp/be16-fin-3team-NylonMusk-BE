package com.beyond.MKX.domain.ipo.ipo.dto;

import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.ipo.entity.IpoStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IpoCreateReqDTO {

    @NotNull
    private String symbol;
    @NotNull @Positive
    private Long faceValue;
    @NotNull @Positive
    private Long totalShares;
    @NotNull
    @DecimalMin("0.0") @DecimalMax("1.0")
    private Double majorShareholderRatio;
    @NotNull
    private Boolean isOffering;

    public Ipo toEntity() {
        return Ipo.builder()
                .symbol(this.symbol)
                .faceValue(this.faceValue)
                .totalShares(this.totalShares)
                .majorShareholderRatio(this.majorShareholderRatio)
                .isOffering(this.isOffering)
                .status(IpoStatus.REQUESTED)       // 기본 상태
                .requestedAt(LocalDateTime.now())  // 생성 시 요청 시각 자동 세팅
                .build();
    }

}
