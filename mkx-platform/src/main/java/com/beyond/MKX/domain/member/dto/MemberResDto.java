package com.beyond.MKX.domain.member.dto;

import com.beyond.MKX.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
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
    private LocalDate birthDate;
    private String status;
    private UUID brokerageId;
    private String brokerageName;
    private String accountNumber;   // 추가: 개인 계좌번호
    private String accountStatus;   // 추가: 개인 계좌 상태
    private UUID accountId;         // 추가: 개인 계좌 ID
    private UUID accountBrokerageId;// 추가: 계좌 소속 증권사 ID
    private Long accountBalance;    // 추가: 잔고
    private Long accountAvailable;  // 추가: 출금 가능 금액
    private LocalDateTime accountCreatedAt; // 추가: 계좌 생성일
    private LocalDateTime createdAt;

    public static MemberResDto from(Member member) {
        return MemberResDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .phone(member.getPhone())
                .birthDate(member.getBirthDate())
                .status(member.getStatus().name())
                .brokerageId(member.getBrokerage() != null ? member.getBrokerage().getId() : null)
                .brokerageName(member.getBrokerage() != null ? member.getBrokerage().getNameKo() : null)
                .createdAt(member.getCreatedAt())
                .build();
    }
}
