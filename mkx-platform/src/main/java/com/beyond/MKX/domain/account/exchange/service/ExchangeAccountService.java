package com.beyond.MKX.domain.account.exchange.service;

import com.beyond.MKX.domain.account.exchange.entity.ExchangeAccount;
import com.beyond.MKX.domain.account.exchange.repository.ExchangeAccountRepository;
import com.beyond.MKX.domain.account.accountlist.entity.AccountList;
import com.beyond.MKX.domain.account.accountlist.entity.AccountType;
import com.beyond.MKX.domain.account.accountlist.service.AccountListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

/**
 * 거래소 계좌 서비스
 *
 * 책임
 * - 거래소(시스템) 계좌 1개를 멱등하게 생성하고 관리한다.
 * - 생성 시 account_list에도 EXCHANGE 유형으로 함께 등록한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExchangeAccountService {

    private final ExchangeAccountRepository repository;
    private final AccountListService accountListService;


     //거래소 계좌 초기 생성
    public ExchangeAccount createExchangeAccount(String accountNumber) {
        AccountList list = accountListService.registerIfAbsent(accountNumber, AccountType.EXCHANGE);
        return repository.save(new ExchangeAccount(accountNumber, list));
    }

    /**
     * 입금 기능 (테스트/샘플용)
     */
    public BigInteger deposit(String accountNumber, BigInteger amount) {
        ExchangeAccount acc = repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("거래소 계좌 없음"));
        acc.deposit(amount);
        return acc.getBalance();
    }

    public BigInteger withdraw(String accountNumber, BigInteger amount) {
        ExchangeAccount acc = repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("거래소 계좌 없음"));
        acc.withdraw(amount);
        return acc.getBalance();
    }

    /** 단건 조회 */
    public ExchangeAccount getByAccountNumber(String accountNumber) {
        return repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("거래소 계좌 없음"));
    }
}
