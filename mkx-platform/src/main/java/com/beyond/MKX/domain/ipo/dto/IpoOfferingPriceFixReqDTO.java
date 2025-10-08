package com.beyond.MKX.domain.ipo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class IpoOfferingPriceFixReqDTO {
    @NotNull
    @Positive
    private Long offerPrice;
}
