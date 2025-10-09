package com.beyond.MKX.domain.account.member.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.account.member.entity.MemberAccount;
import com.beyond.MKX.domain.account.member.service.MemberAccountService;
import com.beyond.MKX.domain.account.admin.client.AdminInternalClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.naming.AuthenticationException;
import java.util.UUID;

/**
 * 관리자용 회원 계좌 상태 변경 API
 *
 * 공개(외부) 엔드포인트: 게이트웨이를 통해 호출된다.
 * 권한 정책:
 *  - EXCHANGE: 모든 회원 계좌 상태 변경 가능
 *  - BROKERAGE: 본인 소속 증권사 소유의 회원 계좌만 상태 변경 가능
 * 인증/권한:
 *  - 게이트웨이가 주입한 헤더(X-User-Role, X-User-Id)를 신뢰한다.
 */
@RestController
@RequestMapping("/api/admin/member-accounts")
@RequiredArgsConstructor
public class MemberAccountAdminController {

    private final MemberAccountService service;
    private final AdminInternalClient adminInternalClient;

    /**
     * 회원 계좌 정지
     * - EXCHANGE: 전체 허용
     * - BROKERAGE: 본인 소속 증권사 계좌만 허용(플랫폼 내부 API로 소속 검증)
     */
    @PostMapping("/{accountNumber}/suspend")
    public ResponseEntity<?> suspend(@RequestHeader(value = "X-User-Role", required = false) String role,
                                     @RequestHeader(value = "X-User-Id", required = false) String adminId,
                                     @PathVariable String accountNumber) throws AuthenticationException {
        // 대상 계좌 조회(소속 증권사 ID 확인 목적)
        MemberAccount acc = service.getByAccountNumber(accountNumber);
        // 역할/소속 증권사 권한 검증
        authorize(role, adminId, acc.getBrokerageId());
        // 상태 변경 수행
        service.updateStatus(acc.getId(), "SUSPEND");
        return ApiResponse.ok(null, "회원 계좌 정지 완료");
    }

    /**
     * 회원 계좌 활성화
     */
    @PostMapping("/{accountNumber}/activate")
    public ResponseEntity<?> activate(@RequestHeader(value = "X-User-Role", required = false) String role,
                                      @RequestHeader(value = "X-User-Id", required = false) String adminId,
                                      @PathVariable String accountNumber) throws AuthenticationException {
        MemberAccount acc = service.getByAccountNumber(accountNumber);
        authorize(role, adminId, acc.getBrokerageId());
        service.updateStatus(acc.getId(), "ACTIVATE");
        return ApiResponse.ok(null, "회원 계좌 활성화 완료");
    }

    /**
     * 회원 계좌 삭제(폐쇄)
     */
    @PostMapping("/{accountNumber}/delete")
    public ResponseEntity<?> delete(@RequestHeader(value = "X-User-Role", required = false) String role,
                                    @RequestHeader(value = "X-User-Id", required = false) String adminId,
                                    @PathVariable String accountNumber) throws AuthenticationException {
        MemberAccount acc = service.getByAccountNumber(accountNumber);
        authorize(role, adminId, acc.getBrokerageId());
        service.updateStatus(acc.getId(), "DELETE");
        return ApiResponse.ok(null, "회원 계좌 삭제(폐쇄) 완료");
    }

    /**
     * 역할/소속 증권사 권한 검증 로직
     *
     * @param role              X-User-Role 헤더(EXCHANGE | BROKERAGE)
     * @param adminId           X-User-Id 헤더(BROKERAGE일 때 필수)
     * @param targetBrokerageId 대상 계좌의 소속 증권사 ID
     * @throws AuthenticationException 권한 부족 또는 헤더 누락 등
     */
    private void authorize(String role, String adminId, UUID targetBrokerageId) throws AuthenticationException {
        // 역할 헤더 필수
        if (role == null || role.isBlank()) {
            throw new AuthenticationException("X-User-Role 헤더가 없습니다.");
        }
        // 거래소 관리자는 전체 허용
        if ("EXCHANGE".equalsIgnoreCase(role)) {
            return;
        }
        // 증권사 관리자는 본인 소속 증권사 계좌만 허용
        if ("BROKERAGE".equalsIgnoreCase(role)) {
            if (adminId == null || adminId.isBlank()) {
                throw new AuthenticationException("X-User-Id 헤더가 없습니다.");
            }
            // 관리자의 소속 증권사 ID 조회(플랫폼 내부 API 호출)
            UUID adminUuid = UUID.fromString(adminId);
            AdminInternalClient.BrokerageIdRes res = adminInternalClient.getBrokerageId(adminUuid);
            // 소속 불일치 시 거부
            if (res == null || res.brokerageId() == null || !res.brokerageId().equals(targetBrokerageId)) {
                throw new AuthenticationException("해당 증권사 소속 계좌만 변경 가능합니다.");
            }
            return;
        }
        // 허용되지 않은 역할(예: MEMBER, CORPORATION 등)
        throw new AuthenticationException("허용되지 않은 역할입니다.");
    }
}
