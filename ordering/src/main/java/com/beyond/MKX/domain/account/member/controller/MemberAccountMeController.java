package com.beyond.MKX.domain.account.member.controller;

import com.beyond.MKX.domain.assets.entity.MemberAccount;
import com.beyond.MKX.domain.assets.repository.MemberAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/accounts/member")
@RequiredArgsConstructor
public class MemberAccountMeController {
    private final MemberAccountRepository repository;

    // JWT → SecurityContext에서 userId/role를 꺼내거나, 게이트웨이가 주입한 헤더를 읽는 유틸 사용
    @GetMapping("/me/latest-id")
    public ResponseEntity<?> getMyLatestMemberAccountId(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        if (role == null || userId == null) {
            return ResponseEntity.status(401).build();
        }
        UUID ownerId = UUID.fromString(userId);

        // 정책:
        // - MEMBER: 본인(memberId)의 최신 계좌
        // - CORPORATION: (조직 정책에 맞게) 해당 기업 소속의 ‘기업형 회원계좌’를 저장하는 테이블/연결이 있다면 그 기준으로 조회
        //   없으면 우선 MEMBER 케이스만 먼저 오픈하고, 이후 CORPORATION 매핑을 붙인다.
        if (!"MEMBER".equalsIgnoreCase(role)) {
            // 필요 시 CORPORATION 전용 조회 로직 확장 (기업-회원계좌 매핑 테이블/뷰 사용)
            return ResponseEntity.status(403).build();
        }

        MemberAccount acc = repository.findFirstByMemberIdOrderByCreatedAtDesc(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("회원 계좌 없음"));

        // 필요한 최소 데이터만
        return ResponseEntity.ok(Map.of(
                "memberAccountId", acc.getId(),
                "accountNumber", acc.getNumber(),
                "status", acc.getStatus().name()
        ));
    }
}
