package com.beyond.MKX.domain.member.dto;

import com.beyond.MKX.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberAdminSummaryDto {

    private UUID id;
    private String name;
    private String email;
    private String brokerageName;
    private String status;

    public static MemberAdminSummaryDto from(Member member) {
        return MemberAdminSummaryDto.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .brokerageName(member.getBrokerage() != null ? member.getBrokerage().getNameKo() : null)
                .status(member.getStatus().name())
                .build();
    }
}
