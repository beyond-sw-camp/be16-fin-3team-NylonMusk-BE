package com.beyond.MKX.domain.account.member.client;

import com.beyond.MKX.domain.account.member.dto.MemberContextRes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * 플랫폼(mkx-platform-service)이 제공하는 "회원 내부 API"를 호출하는 Feign 클라이언트.
 *
 * 사용 목적(대표 시나리오)
 * - 회원 계좌 자동 생성 전, 회원과 소속 증권사의 상태가 ACTIVE인지 검증하고
 *   회원의 소속 증권사 ID(brokerageId)를 확보하기 위해 사용한다.
 *
 * 연결 대상
 * - 서비스명: mkx-platform-service (Eureka 등록명)
 * - 엔드포인트: GET /api/internal/members/{memberId}/context
 */
@FeignClient(name = "mkx-platform-service", contextId = "memberInternalClient", url = "http://mkx-platform-service")
public interface MemberInternalClient {

    /**
     * 회원 컨텍스트 조회(소속 증권사 ID, 회원/증권사 상태 등)
     *
     * @param memberId 회원 UUID
     * @return MemberContextRes (brokerageId, memberStatus, brokerageStatus)
     */
    @GetMapping("/api/internal/members/{memberId}/context")
    MemberContextRes getContext(@PathVariable("memberId") UUID memberId);

    /**
     * 회원 이름 조회
     *
     * @param memberId 회원 UUID
     * @return Map { "name": "회원 이름" }
     */
    @GetMapping("/api/internal/members/{memberId}/name")
    java.util.Map<String, String> getMemberName(@PathVariable("memberId") UUID memberId);
}
