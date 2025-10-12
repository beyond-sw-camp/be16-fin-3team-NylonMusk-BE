package com.beyond.MKX.domain.account.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 회원 컨텍스트 응답 DTO
 *
 * 제공자(Provider): mkx-platform 내부 API
 *  - GET /api/internal/members/{memberId}/context
 *  - 회원의 소속 증권사 ID 및 회원/증권사의 상태를 함께 반환한다.
 *
 * 소비자(Consumer): ordering 서비스
 *  - 회원 계좌 생성 전, memberStatus/brokerageStatus가 ACTIVE인지 검증하고
 *    brokerageId를 이용해 계좌를 생성한다.
 */
@Getter
@NoArgsConstructor
public class MemberContextRes {
    /** 회원이 소속된 증권사(브로커리지) ID */
    private UUID brokerageId;
    /** 회원 상태(예: ACTIVE, SUSPENDED 등) */
    private String memberStatus;
    /** 증권사 상태(예: ACTIVE, SUSPENDED 등) */
    private String brokerageStatus;
}
