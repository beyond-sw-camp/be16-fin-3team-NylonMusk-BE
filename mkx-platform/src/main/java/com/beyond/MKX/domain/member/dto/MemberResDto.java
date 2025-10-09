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
public class MemberResDto {
    private UUID id;
    private String email;
    private String name;
    private String phone;
    private String status;
    private UUID brokerageId;
    private String brokerageName;
    private String accountNumber;   // 추가: 개인 계좌번호
    private String accountStatus;   // 추가: 개인 계좌 상태
    private LocalDateTime createdAt;

    public static MemberResDto from(Member member) {
        return MemberResDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .phone(member.getPhone())
                .status(member.getStatus().name())
                .brokerageId(member.getBrokerage() != null ? member.getBrokerage().getId() : null)
                .brokerageName(member.getBrokerage() != null ? member.getBrokerage().getNameKo() : null)
                .createdAt(member.getCreatedAt())
                .build();
    }
}
