package com.beyond.MKX.domain.ipo.ipo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IpoListReqDTO {
    @Positive(message = "(공모 없이 상장하는 경우) 상장 기준가는 양수여야 합니다.")
    private Long priceOnListing;
}
