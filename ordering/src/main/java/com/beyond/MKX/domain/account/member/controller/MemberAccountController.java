package com.beyond.MKX.domain.account.member.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.account.member.service.IdempotencyService;
import com.beyond.MKX.domain.account.member.dto.MemberAccountCreateRes;
import com.beyond.MKX.domain.account.member.dto.AmountRequest;
import com.beyond.MKX.domain.account.member.dto.TransferRequest;
import com.beyond.MKX.domain.account.member.dto.AccountInfoResponse;
import com.beyond.MKX.domain.account.member.dto.MemberContextRes;
import com.beyond.MKX.domain.account.member.service.MemberAccountService;
import com.beyond.MKX.domain.account.member.client.MemberInternalClient;
import com.beyond.MKX.domain.assets.entity.MemberAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.naming.AuthenticationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 회원 계좌 컨트롤러
 *
 * 역할/흐름
 * - 게이트웨이가 주입한 인증 헤더(X-User-Role, X-User-Id)를 신뢰한다.
 * - 회원(MEMBER) 본인 요청만 허용하고, 회원/증권사 ACTIVE 상태를 "플랫폼"에 Feign으로 조회해 검증한다.
 * - 멱등성: 회원ID + 증권사ID로 서버가 결정적 키(SHA-256)를 생성해 동시/중복 제출을 흡수한다.
 * - 실제 생성 후 "플랫폼"의 account_list에 메타 데이터를 등록한다.
 */
@RestController
@RequestMapping("/api/accounts/member")
@RequiredArgsConstructor
public class MemberAccountController {

    // 도메인 서비스(생성/조회/입출금/상태관리 등)
    private final MemberAccountService service;
    // (Feign) 플랫폼 내부 API: 회원 컨텍스트 조회(브로커리지/상태)
    private final MemberInternalClient memberInternalClient;
    // 멱등 예약/결과 캐시 관리 서비스
    private final IdempotencyService idempotencyService;

    /**
     * 유저 계좌 자동 생성 (회원 전용)
     * 헤더 요구: X-User-Role=MEMBER, X-User-Id=<회원 UUID>
     * 1) 역할/본인 식별 검증 → 2) 플랫폼에 회원 컨텍스트 조회(회원/증권사 ACTIVE) →
     * 3) 서버 파생 멱등키 예약 → 4) 생성 + account_list 등록 → 5) 결과 캐싱
     */
    @PostMapping("/create")
    public ResponseEntity<?> createAuto(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String memberId
    ) throws AuthenticationException {
        // 1) 역할 검증: 회원 전용
        if (role == null || !"MEMBER".equalsIgnoreCase(role)) {
            throw new AuthenticationException("회원만 접근 가능합니다.");
        }
        // 2) 본인 식별자 유효성
        if (memberId == null || memberId.isBlank()) {
            throw new AuthenticationException("X-User-Id 헤더가 없습니다.");
        }
        // 문자열 UUID → 타입 변환
        UUID memberUuid = UUID.fromString(memberId);
        // 3) 회원 컨텍스트 조회(회원/증권사 ACTIVE, 소속 증권사 ID)
        MemberContextRes context = memberInternalClient.getContext(memberUuid);
        if (context == null || context.getBrokerageId() == null) {
            throw new IllegalArgumentException("회원의 증권사 정보가 없습니다.");
        }
        if (!"ACTIVE".equalsIgnoreCase(context.getMemberStatus())) {
            throw new AuthenticationException("회원 상태가 활성(ACTIVE)이 아닙니다.");
        }
        if (!"ACTIVE".equalsIgnoreCase(context.getBrokerageStatus())) {
            throw new AuthenticationException("증권사 상태가 활성(ACTIVE)이 아닙니다.");
        }
        // 4) 멱등 키 파생(서버 측): 회원ID:증권사ID → SHA-256(hex)
        String effectiveKey = deriveIdempotencyKey(memberUuid, context.getBrokerageId());

        // 5) 멱등 예약(PENDING) 선점: 동시/중복 요청 흡수
        // 멱등 예약: 동일 키 요청 중복/동시성 흡수(PENDING) + 완료 결과 재활용
        java.time.Duration pendingTtl = java.time.Duration.ofSeconds(30);
        Optional<String> reserved = idempotencyService.reserveOrGet(effectiveKey, pendingTtl);
        if (reserved.isPresent()) {
            String value = reserved.get();
            if (value != null && value.startsWith("RESULT:")) {
                // 이미 처리 완료된 요청: 새 작업을 만들지 않고 에러로 통일(정책상 에러)
                throw new IllegalStateException("이미 처리된 요청입니다.");
            }
            // 처리 진행 중(PENDING): 클라이언트에 재시도 요청(409/429 등으로 매핑될 수 있음)
            throw new IllegalStateException("요청 처리 중입니다.");
        }

        try {
            // 6) 실제 계좌 생성 + 플랫폼 account_list 등록
            MemberAccount acc = service.createAuto(memberUuid, context.getBrokerageId());
            // 7) 결과 캐싱(RESULT) — 같은 키 재요청 시 기존 결과를 즉시 반환할 수 있도록 저장
            idempotencyService.storeResult(effectiveKey, acc.getNumber(), java.time.Duration.ofHours(24));
            return ApiResponse.ok(new MemberAccountCreateRes(acc.getNumber()), "회원 계좌 생성 완료");
        } catch (IllegalStateException ex) {
            // 이미 존재(UNIQUE 충돌 등): 기존 계좌 조회 후 에러로 통일(정책에 따라 성공 응답으로 변경 가능)
            Optional<MemberAccount> existing = service.findByMemberAndBrokerage(memberUuid, context.getBrokerageId());
            if (existing.isPresent()) {
                String accNo = existing.get().getNumber();
                idempotencyService.storeResult(effectiveKey, accNo, java.time.Duration.ofHours(24));
                throw new IllegalStateException("이미 계좌가 존재합니다.");
            }
            idempotencyService.release(effectiveKey);
            throw ex;
        } catch (RuntimeException ex) {
            // 기타 실패: 예약 해제(다음 재시도 가능하도록)
            idempotencyService.release(effectiveKey);
            throw ex;
        }
    }

    /**
     * 개인 계좌 단건 조회 (계좌번호)
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<?> getByAccountNumber(@PathVariable String accountNumber) {
        MemberAccount acc = service.getByAccountNumber(accountNumber);
        return ApiResponse.ok(acc, "개인 계좌 조회 성공");
    }

    /**
     * 증권사에 속한 개인 계좌 목록 조회
     */
    @GetMapping("/by-brokerage/{brokerageId}")
    public ResponseEntity<?> listByBrokerage(@PathVariable UUID brokerageId) {
        List<MemberAccount> list = service.listByBrokerage(brokerageId);
        return ApiResponse.ok(list, "증권사 소속 개인 계좌 목록 조회 성공");
    }

    /**
     * 서버 파생 멱등 키 생성
     * - 입력: memberId + ":" + brokerageId
     * - 알고리즘: SHA-256 → hex 문자열
     */
    private String deriveIdempotencyKey(UUID memberId, UUID brokerageId) {
        String raw = memberId + ":" + brokerageId;
        try {
            // SHA-256 알고리즘을 사용할 MessageDigest 인스턴스 생성
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 입력 문자열(raw)을 UTF-8로 바이트 변환 후 해시 계산
            // digest()는 32바이트(256비트)의 바이트 배열을 반환
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            // 바이트 배열을 사람이 읽을 수 있는 16진수 문자열로 변환
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            //  SHA-256 알고리즘이 없는 경우 (거의 발생하지 않음)
            throw new IllegalStateException("서버 키 생성 실패", e);
        }
    }

    /**
     * 개인 계좌 입금 (회원 본인만)
     * 헤더 요구: X-User-Role=MEMBER, X-User-Id=<회원 UUID>
     * - 본인 계좌 여부 검증
     * - 서비스 레벨에서 계좌 상태 ACTIVE 검증
     */
    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<?> deposit(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String memberId,
            @PathVariable String accountNumber,
            @RequestBody AmountRequest req
    ) throws AuthenticationException {
        if (role == null || (!"MEMBER".equalsIgnoreCase(role) && !"INTERNAL".equalsIgnoreCase(role))) {
            throw new AuthenticationException("회원만 접근 가능합니다.");
        }
        if (memberId == null || memberId.isBlank()) {
            throw new AuthenticationException("X-User-Id 헤더가 없습니다.");
        }
        MemberAccount acc = service.getByAccountNumber(accountNumber);
        if (!acc.getMemberId().equals(UUID.fromString(memberId))) {
            throw new AuthenticationException("본인 계좌가 아닙니다.");
        }
        Long balance = service.deposit(accountNumber, req.getAmount());
        return ApiResponse.ok(Map.of("balance", balance), "입금 완료");
    }

    /**
     * 개인 계좌 출금 (회원 본인만)
     * 헤더 요구: X-User-Role=MEMBER, X-User-Id=<회원 UUID>
     * - 본인 계좌 여부 검증
     * - 서비스 레벨에서 계좌 상태 ACTIVE 검증 및 잔액 부족 검사
     */
    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<?> withdraw(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String memberId,
            @PathVariable String accountNumber,
            @RequestBody AmountRequest req
    ) throws AuthenticationException {
        if (role == null || (!"MEMBER".equalsIgnoreCase(role) && !"INTERNAL".equalsIgnoreCase(role))) {
            throw new AuthenticationException("회원만 접근 가능합니다.");
        }
        if (memberId == null || memberId.isBlank()) {
            throw new AuthenticationException("X-User-Id 헤더가 없습니다.");
        }
        MemberAccount acc = service.getByAccountNumber(accountNumber);
        if (!"INTERNAL".equalsIgnoreCase(role)) {
            if (!acc.getMemberId().equals(UUID.fromString(memberId))) {
                throw new AuthenticationException("본인 계좌가 아닙니다.");
            }
        }
        Long balance = service.withdraw(accountNumber, req.getAmount());
        return ApiResponse.ok(Map.of("balance", balance), "출금 완료");
    }

    /**
     * 계좌 이체 (회원 본인만)
     * 헤더 요구: X-User-Role=MEMBER, X-User-Id=<회원 UUID>
     * - 송금인 본인 계좌 여부 검증
     * - 수취인 계좌 존재 여부 자동 검증
     */
    @PostMapping("/{fromAccountNumber}/transfer")
    public ResponseEntity<?> transfer(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String memberId,
            @PathVariable String fromAccountNumber,
            @RequestBody TransferRequest req
    ) throws AuthenticationException {
        if (role == null || !"MEMBER".equalsIgnoreCase(role)) {
            throw new AuthenticationException("회원만 접근 가능합니다.");
        }
        if (memberId == null || memberId.isBlank()) {
            throw new AuthenticationException("X-User-Id 헤더가 없습니다.");
        }
        
        // 송금인 본인 계좌 여부 검증
        MemberAccount fromAcc = service.getByAccountNumber(fromAccountNumber);
        if (!fromAcc.getMemberId().equals(UUID.fromString(memberId))) {
            throw new AuthenticationException("본인 계좌가 아닙니다.");
        }
        
        // 이체 실행
        service.transfer(fromAccountNumber, req.getToAccountNumber(), req.getAmount());
        
        // 이체 후 잔액 조회
        MemberAccount updatedAcc = service.getByAccountNumber(fromAccountNumber);
        return ApiResponse.ok(Map.of("balance", updatedAcc.getBalance()), "이체 완료");
    }

    /**
     * 계좌 정보 조회 (계좌번호로 소유자 확인)
     * - 모든 로그인 사용자가 조회 가능 (이체 시 수취인 확인용)
     * - 현재는 계좌 존재 여부만 확인 (추후 이름 조회 기능 추가 가능)
     */
    @GetMapping("/info/{accountNumber}")
    public ResponseEntity<?> getAccountInfo(@PathVariable String accountNumber) {
        MemberAccount acc = service.getByAccountNumber(accountNumber);
        
        // 회원 ID를 마스킹하여 일부만 표시
        String memberId = acc.getMemberId().toString();
        String maskedMemberId = memberId.substring(0, 8) + "****";
        
        AccountInfoResponse response = new AccountInfoResponse(
            accountNumber,
            "회원 (" + maskedMemberId + ")",  // 임시: 회원 ID 일부만 표시
            "MEMBER"
        );
        
        return ApiResponse.ok(response, "계좌 정보 조회 성공");
    }
}
