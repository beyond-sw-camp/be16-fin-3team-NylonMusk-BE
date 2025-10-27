package com.beyond.MKX.domain.account.member.controller;

import com.beyond.MKX.domain.account.member.dto.AmountRequest;
import com.beyond.MKX.domain.account.member.dto.MemberAccountSummary;
import com.beyond.MKX.domain.account.member.service.MemberAccountService;
import com.beyond.MKX.domain.assets.entity.MemberAccount;
import com.beyond.MKX.domain.assets.repository.MemberAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 내부 전용 회원 계좌 조회 API(오더링 → 플랫폼 등 내부 호출용)
 * - 외부로 공개되지 않아야 하며, 게이트웨이 레벨에서 접근 차단이 전제된다.
 * - 최근 생성된 회원 계좌 1개(요약)를 돌려준다.
 */
@RestController
@RequestMapping("/api/internal/member-accounts")
@RequiredArgsConstructor
public class MemberAccountInternalController {

    private final MemberAccountRepository repository;
    private final MemberAccountService service;

    /**
     * 회원의 최근 계좌 요약 조회
     * @param memberId 회원 UUID
     * @return 계좌번호/상태 요약
     */
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberAccountSummary> getByMember(@PathVariable UUID memberId) {
        MemberAccount account = repository.findFirstByMemberIdOrderByCreatedAtDesc(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 계좌 없음"));
        return ResponseEntity.ok(MemberAccountSummary.builder()
                .accountNumber(account.getNumber())
                .status(account.getStatus())
                .build());
    }

    /**
     * 계좌번호로 회원 계좌 소유자/소속 요약 조회 (내부용)
     */
    @GetMapping("/by-number/{accountNumber}")
    public ResponseEntity<MemberAccountByNumberRes> getByAccountNumber(@PathVariable String accountNumber) {
        MemberAccount account = repository.findByNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("회원 계좌 없음"));
        return ResponseEntity.ok(new MemberAccountByNumberRes(
                account.getId(),
                account.getMemberId(),
                account.getBrokerageId(),
                account.getNumber(),
                account.getStatus().name()
        ));
    }

    public record MemberAccountByNumberRes(
            UUID accountId,
            UUID memberId,
            UUID brokerageId,
            String accountNumber,
            String status
    ) {}

    /**
     * 회원의 최근 계좌 상세 조회 (내부용)
     * - 플랫폼 등 내부 시스템이 회원/계좌 상세를 합성하기 위해 사용
     */
    @GetMapping("/{memberId}/detail")
    public ResponseEntity<MemberAccountDetailRes> getDetailByMember(@PathVariable UUID memberId) {
        MemberAccount account = repository.findFirstByMemberIdOrderByCreatedAtDesc(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 계좌 없음"));
        return ResponseEntity.ok(new MemberAccountDetailRes(
                account.getId(),
                account.getMemberId(),
                account.getBrokerageId(),
                account.getNumber(),
                account.getStatus().name(),
                account.getBalance(),
                account.getAvailableBalance(),
                account.getCreatedAt()
        ));
    }

    public record MemberAccountDetailRes(
            UUID accountId,
            UUID memberId,
            UUID brokerageId,
            String accountNumber,
            String status,
            Long balance,
            Long availableBalance,
            LocalDateTime createdAt
    ) {}

    // 내부 시스템용 자금 이동 API 추가 (mkx-platform → ordering Feign 호출용)

    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<Long> internalWithdraw(@PathVariable String accountNumber,
                                                 @RequestBody AmountRequest req) {
        Long balance = service.withdraw(accountNumber, req.getAmount());
        return ResponseEntity.ok(balance);
    }

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<Long> internalDeposit(@PathVariable String accountNumber,
                                                @RequestBody AmountRequest req) {
        Long balance = service.deposit(accountNumber, req.getAmount());
        return ResponseEntity.ok(balance);
    }

}
