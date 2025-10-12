package com.beyond.MKX.domain.account.accountlist.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.ExchangeOnly;
import com.beyond.MKX.domain.account.accountlist.dto.AccountListAdminItemDto;
import com.beyond.MKX.domain.account.accountlist.entity.AccountList;
import com.beyond.MKX.domain.account.accountlist.service.AccountListService;
import com.beyond.MKX.domain.account.brokerage.repository.BrokerageDepositAccountRepository;
import com.beyond.MKX.domain.account.corporation.repository.CorporationAccountRepository;
import com.beyond.MKX.domain.account.exchange.repository.ExchangeAccountRepository;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
import com.beyond.MKX.domain.member.client.MemberAccountInternalClient;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import com.beyond.MKX.domain.securities_firm.repository.SecuritiesFirmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 거래소 관리자용 account_list 조회 컨트롤러
 * - @ExchangeOnly 보호됨 (EXCHANGE 관리자만 접근 가능)
 * - type, status, search 쿼리 파라미터로 필터링 가능
 */
@RestController
@RequestMapping("/admin/account-list")
@RequiredArgsConstructor
public class AccountListAdminController {

    private final AccountListService accountListService;
    private final BrokerageDepositAccountRepository brokerageDepositAccountRepository;
    private final CorporationAccountRepository corporationAccountRepository;
    private final ExchangeAccountRepository exchangeAccountRepository;
    private final SecuritiesFirmRepository securitiesFirmRepository;
    private final CorporationRepository corporationRepository;
    private final MemberAccountInternalClient memberAccountInternalClient;
    private final MemberRepository memberRepository;

    /**
     * 📄 account_list 목록 조회
     *   - type: EXCHANGE | BROKERAGE | CORPORATION | MEMBER (optional)
     *   - status: PENDING | APPROVED | REJECTED | SUSPENDED (optional)
     *   - search: 계좌번호 부분 검색 (optional)
     */
    @ExchangeOnly
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search
    ) {
        // 1️⃣ 필터링된 기본 목록 조회
        List<AccountList> accountLists = accountListService.search(type, status, search);

        // 2️⃣ 각 계좌 타입별로 세부정보 enrich
        List<AccountListAdminItemDto> enrichedList = accountLists.stream()
                .map(account -> {

                    // 기본 빌더 초기화
                    AccountListAdminItemDto.AccountListAdminItemDtoBuilder builder =
                            AccountListAdminItemDto.builder()
                                    .accountNumber(account.getAccountNumber())
                                    .type(account.getType().name())
                                    .status(account.getStatus().name())
                                    .createdAt(account.getCreatedAt());

                    switch (account.getType()) {

                        // 🏦 증권사 계좌
                        case BROKERAGE -> brokerageDepositAccountRepository
                                .findByAccountNumber(account.getAccountNumber())
                                .ifPresent(depositAccount ->
                                        securitiesFirmRepository.findById(depositAccount.getBrokerageId())
                                                .ifPresent(securitiesFirm ->
                                                        builder.brokerageName(securitiesFirm.getNameKo())
                                                )
                                );

                        // 🏢 법인 계좌
                        case CORPORATION -> corporationAccountRepository
                                .findByAccountNumber(account.getAccountNumber())
                                .ifPresent(corpAccount ->
                                        corporationRepository.findById(corpAccount.getCorporationId())
                                                .ifPresent(corporation ->
                                                        builder.corporationName(corporation.getNameKo())
                                                )
                                );

                        // 👤 회원 계좌
                        case MEMBER -> {
                            try {
                                MemberAccountInternalClient.MemberAccountByNumberRes memberRes =
                                        memberAccountInternalClient.getByAccountNumber(account.getAccountNumber());

                                if (memberRes != null) {
                                    // 회원 이름/이메일 추가
                                    memberRepository.findById(memberRes.memberId())
                                            .ifPresent(member ->
                                                    builder.memberName(member.getName())
                                                            .memberEmail(member.getEmail())
                                            );

                                    // 소속 증권사 이름 추가
                                    securitiesFirmRepository.findById(memberRes.brokerageId())
                                            .ifPresent(sf -> builder.brokerageName(sf.getNameKo()));
                                }
                            } catch (Exception ignore) {
                                // 내부 통신 실패 시 무시
                            }
                        }

                        // 💹 거래소 계좌
                        case EXCHANGE -> builder.brokerageName("거래소");
                    }

                    return builder.build();
                })
                .toList();

        // 3️⃣ 응답 래핑
        return ApiResponse.ok(enrichedList, "account_list 목록 조회");
    }
}
