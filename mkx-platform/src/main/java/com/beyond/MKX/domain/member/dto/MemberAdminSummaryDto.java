package com.beyond.MKX.domain.member.dto;

import com.beyond.MKX.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberAdminSummaryDto {

    private UUID id;
    private String name;
    private String email;
    private String brokerageName;
    private String status;
    private LocalDateTime createdAt; // 가입일
    private String accountNumber;    // 최신 계좌번호(요약)


    public static MemberAdminSummaryDto from(Member member) {
        return MemberAdminSummaryDto.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .brokerageName(member.getBrokerage() != null ? member.getBrokerage().getNameKo() : null)
                .status(member.getStatus().name())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
