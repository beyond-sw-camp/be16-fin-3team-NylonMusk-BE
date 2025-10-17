package com.beyond.MKX.domain.ipo.ipo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IpoReviewReqDTO {
    @NotNull
    private Boolean approve;
    private String rejectReason;
}
