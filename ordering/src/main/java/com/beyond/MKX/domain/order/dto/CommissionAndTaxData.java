package com.beyond.MKX.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommissionAndTaxData {
    private Long commission;
    private Long tax;
}
