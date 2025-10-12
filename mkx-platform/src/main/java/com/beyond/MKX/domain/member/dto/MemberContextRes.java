package com.beyond.MKX.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * 회원 컨텍스트 응답 DTO (플랫폼 → 오더링 제공)
 *
 * 목적
 * - 오더링 서비스가 회원 계좌를 생성/검증할 때,
 *   회원의 소속 증권사 ID 및 회원/증권사의 현재 상태를 확인할 수 있도록 전달한다.
 *
 * 사용처(주요)
 * - ordering: MemberInternalClient#getContext(memberId)
 *   - memberStatus/brokerageStatus가 ACTIVE인지 검증
 *   - brokerageId를 이용해 계좌 생성
 */
@Getter
@AllArgsConstructor
public class MemberContextRes {
    /** 회원이 소속된 증권사(브로커리지) ID */
    private UUID brokerageId;
    /** 회원 상태(예: ACTIVE, SUSPENDED 등) */
    private String memberStatus;
    /** 증권사 상태(예: ACTIVE, SUSPENDED 등) */
    private String brokerageStatus;
}
