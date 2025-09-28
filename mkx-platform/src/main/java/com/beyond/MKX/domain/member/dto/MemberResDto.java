package com.beyond.MKX.domain.member.dto;

import com.beyond.MKX.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResDto {
    private UUID id;
    private String email;
    private String name;
    private String phone;
    private String position;
    private String role;
    private String userStatus;
    private String organizationName;

    public static MemberResDto from(Member member) {
        return MemberResDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .phone(member.getPhone())
                .position(member.getPosition())
                .role(member.getRole().name())
                .userStatus(member.getUserStatus().name())
                .organizationName(member.getOrganization().getNameKo())
                .build();
    }
}
