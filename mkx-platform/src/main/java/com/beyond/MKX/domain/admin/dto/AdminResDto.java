package com.beyond.MKX.domain.admin.dto;

import com.beyond.MKX.domain.admin.entity.Admin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminResDto {
    private UUID id;
    private String email;
    private String name;
    private String phone;
    private String role;

    public static AdminResDto from(Admin admin) {
        return AdminResDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .name(admin.getName())
                .phone(admin.getPhone())
                .role(admin.getRole().name())
                .build();
    }
}
