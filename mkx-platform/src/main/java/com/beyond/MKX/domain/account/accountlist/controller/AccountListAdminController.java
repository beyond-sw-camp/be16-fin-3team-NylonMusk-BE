package com.beyond.MKX.domain.account.accountlist.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.ExchangeOnly;
import com.beyond.MKX.domain.account.corporation.entity.AccountStatus;
import com.beyond.MKX.domain.account.accountlist.entity.AccountList;
import com.beyond.MKX.domain.account.accountlist.entity.AccountType;
import com.beyond.MKX.domain.account.accountlist.service.AccountListService;
import com.beyond.MKX.domain.account.accountlist.dto.AccountListAdminItemDto;
import com.beyond.MKX.domain.account.brokerage.repository.BrokerageDepositAccountRepository;
import com.beyond.MKX.domain.account.corporation.repository.CorporationAccountRepository;
import com.beyond.MKX.domain.account.exchange.repository.ExchangeAccountRepository;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
import com.beyond.MKX.domain.securities_firm.repository.SecuritiesFirmRepository;
import com.beyond.MKX.domain.member.client.MemberAccountInternalClient;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 거래소 관리자용 account_list 조회 컨트롤러
 *
 * 동작
 * - 스프링 MVC `@RestController`로 등록되며, `/admin/account-list` 경로에 매핑됩니다.
 * - `@ExchangeOnly`로 보호되어 거래소 관리자 권한(토큰)이 있어야 접근 가능합니다.
 * - GET + 쿼리 파라미터(type/status/search)로 필터링해 조회합니다.
 *
 * 사용 예시
 * - GET /admin/account-list
 * - GET /admin/account-list?type=CORPORATION
 * - GET /admin/account-list?status=PENDING
 * - GET /admin/account-list?type=BROKERAGE&status=APPROVED
 * - GET /admin/account-list?search=701-
 */
@RestController
@RequestMapping("/admin/account-list")
@RequiredArgsConstructor
public class AccountListAdminController {

    private final AccountListService service;
    private final BrokerageDepositAccountRepository brokerageDepositAccountRepository;
    private final CorporationAccountRepository corporationAccountRepository;
    private final ExchangeAccountRepository exchangeAccountRepository;
    private final SecuritiesFirmRepository securitiesFirmRepository;
    private final CorporationRepository corporationRepository;
    private final MemberAccountInternalClient memberAccountInternalClient;
    private final MemberRepository memberRepository;

    /**
     * account_list 목록 조회
     * - type: EXCHANGE | BROKERAGE | CORPORATION | MEMBER (optional)
     * - status: PENDING | APPROVED | REJECTED | SUSPENDED (optional)
     * - search: 계좌번호 부분 일치 검색어 (optional)
     * 응답: ApiResponse(data=[AccountList...], message)
     */
    @ExchangeOnly
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search
    ) {
        List<AccountList> rows = service.search(type, status, search);
        java.util.List<AccountListAdminItemDto> enriched = rows.stream().map(a -> {
            var b = AccountListAdminItemDto.builder()
                    .accountNumber(a.getAccountNumber())
                    .type(a.getType().name())
                    .status(a.getStatus().name())
                    .createdAt(a.getCreatedAt());
            switch (a.getType()) {
                case BROKERAGE -> brokerageDepositAccountRepository.findByAccountNumber(a.getAccountNumber()).ifPresent(acc ->
                        securitiesFirmRepository.findById(acc.getBrokerageId()).ifPresent(sf -> b.brokerageName(sf.getNameKo()))
                );
                case CORPORATION -> corporationAccountRepository.findByAccountNumber(a.getAccountNumber()).ifPresent(acc ->
                        corporationRepository.findById(acc.getCorporationId()).ifPresent(corp -> b.corporationName(corp.getNameKo()))
                );
                case MEMBER -> {
                    try {
                        var res = memberAccountInternalClient.getByAccountNumber(a.getAccountNumber());
                        if (res != null) {
                            memberRepository.findById(res.memberId()).ifPresent(m -> b.memberName(m.getName()).memberEmail(m.getEmail()));
                            securitiesFirmRepository.findById(res.brokerageId()).ifPresent(sf -> b.brokerageName(sf.getNameKo()));
                        }
                    } catch (Exception ignore) {}
                }
                case EXCHANGE -> b.brokerageName("거래소");
            }
            return b.build();
        }).toList();
        return ApiResponse.ok(enriched, "account_list 목록 조회");
    }
}
