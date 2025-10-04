package com.beyond.MKX.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class RejectRequestDto {

    @NotBlank(message = "거절 사유는 필수입니다.")
    @Size(max = 255, message = "거절 사유는 255자 이하여야 합니다.")
    private String reason;
}
