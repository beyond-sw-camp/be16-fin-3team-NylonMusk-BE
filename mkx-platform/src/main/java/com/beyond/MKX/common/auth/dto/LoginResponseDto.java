package com.beyond.MKX.common.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class LoginResponseDto {
    private UUID userId;
    private String email;
    private String role;
    private String status;
    private UUID corporationId;
}
