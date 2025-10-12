package com.beyond.MKX.domain.account.brokerage.service;

import com.beyond.MKX.domain.account.brokerage.entity.BrokerageDepositAccount;
import com.beyond.MKX.domain.account.brokerage.repository.BrokerageDepositAccountRepository;
import com.beyond.MKX.domain.account.accountlist.entity.AccountList;
import com.beyond.MKX.domain.account.accountlist.entity.AccountType;
import com.beyond.MKX.domain.account.accountlist.service.AccountListService;
import com.beyond.MKX.domain.account.accountlist.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 증권사 예치금 계좌 서비스
 *
 * 책임
 * - 거래소가 증권사별로 관리하는 예치금 계좌를 생성한다.
 * - 생성 시 account_list에도 BROKERAGE 유형으로 함께 등록한다.
 * - createAuto는 충돌 회피를 포함한 간편 생성(멱등: 존재 시 그대로 반환)이다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BrokerageDepositAccountService {

    private final BrokerageDepositAccountRepository repository;
    private final AccountListService accountListService;

    /**
     * 계좌번호를 자동 생성하여 예치금 계좌를 만든다. (멱등: 이미 존재하면 그대로 반환)
     */
    public BrokerageDepositAccount createAuto(UUID brokerageId) {
        return repository.findByBrokerageId(brokerageId)
                .orElseGet(() -> doCreateAuto(brokerageId));
    }

    private BrokerageDepositAccount doCreateAuto(UUID brokerageId) {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = AccountNumberGenerator.brokerageDeposit(brokerageId, attempt);
            if (repository.findByAccountNumber(candidate).isPresent()) {
                continue; // 충돌 → 재시도
            }
            AccountList list = accountListService.registerIfAbsent(candidate, AccountType.BROKERAGE);
            return repository.save(new BrokerageDepositAccount(brokerageId, candidate, list));
        }
        throw new IllegalStateException("예치금 계좌번호 생성 실패: 충돌 과다");
    }

    /** 단건 조회 */
    public BrokerageDepositAccount getByAccountNumber(String accountNumber) {
        return repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("증권사 예치금 계좌 없음"));
    }

    public java.math.BigInteger deposit(String accountNumber, java.math.BigInteger amount) {
        BrokerageDepositAccount acc = getByAccountNumber(accountNumber);
        acc.deposit(amount);
        return acc.getDeposit();
    }

    public java.math.BigInteger withdraw(String accountNumber, java.math.BigInteger amount) {
        BrokerageDepositAccount acc = getByAccountNumber(accountNumber);
        acc.withdraw(amount);
        return acc.getDeposit();
    }
}
