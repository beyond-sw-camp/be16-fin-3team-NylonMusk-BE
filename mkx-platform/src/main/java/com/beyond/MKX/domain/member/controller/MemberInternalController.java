package com.beyond.MKX.domain.member.controller;

import com.beyond.MKX.domain.member.dto.MemberContextRes;
import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * 내부 전용 Member 조회 API
 * - 주문서비스 등 내부 시스템이 회원 소속 증권사 및 상태를 조회할 때 사용
 */
@RestController
@RequestMapping("/api/internal/members")
@RequiredArgsConstructor
public class MemberInternalController {

    private final MemberRepository memberRepository;

    /**
     * 회원이 소속된 증권사 ID 조회
     *
     * 내부 시스템(주문/정산 등)이 멤버의 소속 증권사 식별자가 필요한 경우 사용합니다.
     * 존재하지 않는 회원 ID가 전달되면 400(Bad Request) 예외가 발생합니다.
     *
     * @param memberId 회원 UUID
     * @return { "brokerageId": <증권사 UUID> }
     */
    @GetMapping("/{memberId}/brokerage-id")
    public ResponseEntity<?> getBrokerageId(@PathVariable UUID memberId) {
        // 회원 조회: 없으면 예외 발생 (전역 예외 처리기로 400 응답 매핑 가정)
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        // 소속 증권사 ID만 경량 응답으로 반환
        return ResponseEntity.ok(Map.of("brokerageId", member.getBrokerage().getId()));
    }

    /**
     * 회원 컨텍스트 조회
     *
     * 주문 등 도메인에서 멤버의 소속 증권사 및 각 상태를 한 번에 확인할 때 사용합니다.
     *
     * @param memberId 회원 UUID
     * @return Member와 Brokerage의 핵심 컨텍스트(증권사 ID, 회원 상태, 증권사 상태)
     */
    @GetMapping("/{memberId}/context")
    public ResponseEntity<MemberContextRes> getContext(@PathVariable UUID memberId) {
        // 회원 조회: 없으면 예외 발생
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        // 멤버가 소속된 증권사 엔티티
        com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm brokerage = member.getBrokerage();
        // 컨텍스트 DTO 구성해 반환
        return ResponseEntity.ok(new MemberContextRes(
                brokerage.getId(),
                member.getStatus().name(),
                brokerage.getStatus().name()
        ));
    }

    /**
     * 회원 이름 조회
     *
     * 계좌 이체 등에서 계좌 소유자의 이름을 확인할 때 사용합니다.
     *
     * @param memberId 회원 UUID
     * @return { "name": "회원 이름" }
     */
    @GetMapping("/{memberId}/name")
    public ResponseEntity<?> getMemberName(@PathVariable UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        return ResponseEntity.ok(Map.of("name", member.getName()));
    }
}
