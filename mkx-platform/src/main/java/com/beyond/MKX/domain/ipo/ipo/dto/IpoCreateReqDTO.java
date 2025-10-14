package com.beyond.MKX.domain.ipo.ipo.dto;

import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.ipo.entity.IpoStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

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
    private Long preOutstandingShares;
    @NotNull
    private MultipartFile preShareholdersFile;
    @NotNull
    private MultipartFile financialStatements;
    @NotNull
    private Boolean isOffering;

    public Ipo toEntity(Corporation corporation) {
        return Ipo.builder()
                .symbol(this.symbol)
                .faceValue(this.faceValue)
                .preOutstandingShares(this.preOutstandingShares)
                .isOffering(this.isOffering)
                .status(IpoStatus.REQUESTED)       // 기본 상태
                .requestedAt(LocalDateTime.now())  // 생성 시 요청 시각 자동 세팅
                .corporation(corporation)
                .build();
    }

}
