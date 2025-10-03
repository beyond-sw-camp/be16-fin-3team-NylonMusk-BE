package com.beyond.MKX.domain.admin.dto;

import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.entity.Status;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AdminSummaryDto {
    private UUID adminId;
    private String name;
    private String email;
    private String phone;
    private Status status;

    public static AdminSummaryDto from(Admin admin) {
        return AdminSummaryDto.builder()
                .adminId(admin.getId())
                .name(admin.getName())
                .email(admin.getEmail())
                .phone(admin.getPhone())
                .status(admin.getStatus())
                .build();
    }
}
