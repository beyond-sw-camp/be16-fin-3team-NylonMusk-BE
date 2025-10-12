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
    private String accountNumber; // 선택: 소속 조직의 계좌번호
    private String accountStatus; // 선택: 계좌 상태(또는 account_list 상태)

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
