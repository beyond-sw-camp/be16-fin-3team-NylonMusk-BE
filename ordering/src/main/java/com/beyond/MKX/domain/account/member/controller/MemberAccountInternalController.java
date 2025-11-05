package com.beyond.MKX.domain.account.member.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.account.member.dto.AmountRequest;
import com.beyond.MKX.domain.account.member.dto.MemberAccountSummary;
import com.beyond.MKX.domain.account.member.service.MemberAccountService;
import com.beyond.MKX.domain.assets.entity.MemberAccount;
import com.beyond.MKX.domain.assets.repository.MemberAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 내부 전용 회원 계좌 조회 API(오더링 → 플랫폼 등 내부 호출용)
 * - 외부로 공개되지 않아야 하며, 게이트웨이 레벨에서 접근 차단이 전제된다.
 * - 최근 생성된 회원 계좌 1개(요약)를 돌려준다.
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/member-accounts")
@RequiredArgsConstructor
public class MemberAccountInternalController {

    private final MemberAccountRepository repository;
    private final MemberAccountService memberAccountService;

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


    
    /**
     * memberAccountId로 계좌번호 조회
     */
    @GetMapping("/{memberAccountId}/account-number")
    public ResponseEntity<?> getAccountNumber(@PathVariable UUID memberAccountId) {
        MemberAccount account = repository.findById(memberAccountId)
                .orElse(null);
        
        if (account == null) {
            log.warn("회원 계좌를 찾을 수 없음: memberAccountId={}", memberAccountId);
            return ApiResponse.ok(Map.of(
                    "success", false,
                    "message", "회원 계좌를 찾을 수 없습니다",
                    "accountNumber", null
            ), "계좌 조회 실패");
        }
        
        log.info("계좌번호 조회 성공: memberAccountId={}, accountNumber={}", memberAccountId, account.getNumber());
        return ApiResponse.ok(Map.of(
                "success", true,
                "message", "조회 성공",
                "accountNumber", account.getNumber()
        ), "조회 성공");
    }

    /**
     * 내부용 회원 계좌 입금 (상장폐지 보상금 지급 등)
     * memberAccountId를 기준으로 계좌를 찾아 입금 처리
     */
    @PostMapping("/{memberAccountId}/deposit")
    public ResponseEntity<?> depositInternal(
            @PathVariable UUID memberAccountId,
            @RequestBody DepositInternalRequest request
    ) {
        // memberAccountId로 계좌 조회 시도
        MemberAccount account = repository.findById(memberAccountId)
                .orElse(null);
        
        // ID로 찾지 못하면 null 반환
        if (account == null) {
            log.warn("회원 계좌를 찾을 수 없음: memberAccountId={}", memberAccountId);
            return ApiResponse.ok(Map.of(
                    "success", false,
                    "message", "회원 계좌를 찾을 수 없습니다"
            ), "계좌 조회 실패");
        }
        
        Long balance = memberAccountService.deposit(account.getNumber(), request.amount());
        
        return ApiResponse.ok(Map.of(
                "success", true,
                "message", "보상금 입금 완료",
                "newBalance", balance
        ), "입금 완료");
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
    
    /**
     * 내부용 회원 계좌 입금 (상장폐지 보상금 지급 등) - 계좌번호 기준
     * ⚠️ description이 있으면 이벤트 발행, 없으면 이벤트 발행 없음 (외부에서 발행)
     */
    @PostMapping("/by-number/{accountNumber}/deposit")
    public ResponseEntity<?> depositByAccountNumber(
            @PathVariable String accountNumber,
            @RequestBody AmountRequest request
    ) {
        MemberAccount account = repository.findByNumber(accountNumber)
                .orElse(null);
        
        if (account == null) {
            log.warn("회원 계좌를 찾을 수 없음: accountNumber={}", accountNumber);
            return ApiResponse.ok(Map.of(
                    "success", false,
                    "message", "회원 계좌를 찾을 수 없습니다"
            ), "계좌 조회 실패");
        }
        
        // description이 있으면 이벤트 발행, 없으면 이벤트 없이 입금 (상장폐지 등 외부에서 이미 이벤트 발행)
        Long balance;
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            // transactionType이 지정된 경우 이벤트 발행, ticker도 함께 전달
            balance = memberAccountService.deposit(accountNumber, request.getAmount(), request.getDescription(), request.getTicker());
        } else {
            // transactionType이 없으면 이벤트 발행 없이 입금 (외부에서 이미 이벤트 발행)
            balance = memberAccountService.depositWithoutEvent(accountNumber, request.getAmount());
        }
        
        return ApiResponse.ok(Map.of(
                "success", true,
                "message", "보상금 입금 완료",
                "newBalance", balance
        ), "입금 완료");
    }

    /**
     * 내부용 회원 계좌 출금 (정산, 환불 등) - 계좌번호 기준
     */
    @PostMapping("/by-number/{accountNumber}/withdraw")
    public ResponseEntity<?> withdrawByAccountNumber(
            @PathVariable String accountNumber,
            @RequestBody AmountRequest request
    ) {
        MemberAccount account = repository.findByNumber(accountNumber)
                .orElse(null);

        if (account == null) {
            log.warn("회원 계좌를 찾을 수 없음: accountNumber={}", accountNumber);
            return ApiResponse.ok(Map.of(
                    "success", false,
                    "message", "회원 계좌를 찾을 수 없습니다"
            ), "계좌 조회 실패");
        }

        // description을 transactionType으로 전달, ticker도 함께 전달
        Long balance = memberAccountService.withdraw(accountNumber, request.getAmount(), request.getDescription(), request.getTicker());

        return ApiResponse.ok(Map.of(
                "success", true,
                "message", "출금 완료",
                "newBalance", balance
        ), "출금 완료");
    }

    public record DepositInternalRequest(
            Long amount
    ) {}

}
