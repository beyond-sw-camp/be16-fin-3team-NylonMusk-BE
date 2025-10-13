package com.beyond.MKX.domain.ipo.ipo.dto;

import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpoCreateResDTO {
    private UUID ipoId;
    private String symbol;
    private Long faceValue;
    private Long preOutstandingShares;
    private Boolean isOffering;
    private String preShareholdersFileUrl;
    private String financialStatementsUrl;

    public static IpoCreateResDTO from(Ipo ipo) {
        return IpoCreateResDTO.builder()
                .ipoId(ipo.getId())
                .symbol(ipo.getSymbol())
                .faceValue(ipo.getFaceValue())
                .preOutstandingShares(ipo.getPreOutstandingShares())
                .isOffering(ipo.getIsOffering())
                .preShareholdersFileUrl(ipo.getPreShareholdersFileUrl())
                .financialStatementsUrl(ipo.getFinancialStatementsUrl())
                .build();
    }
}
