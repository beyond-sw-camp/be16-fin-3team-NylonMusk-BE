package com.beyond.MKX.domain.securities_firm.dto;

import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuritiesFirmSummaryDto {
    private UUID id;
    private String nameKo;
    private String nameEng;

    public static SecuritiesFirmSummaryDto from(SecuritiesFirm firm) {
        return SecuritiesFirmSummaryDto.builder()
                .id(firm.getId())
                .nameKo(firm.getNameKo())
                .nameEng(firm.getNameEng())
                .build();
    }
}