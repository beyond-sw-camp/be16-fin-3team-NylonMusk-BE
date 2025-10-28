package com.beyond.MKX.domain.account.corporation.service;

import com.beyond.MKX.domain.account.corporation.entity.AccountStatus;
import com.beyond.MKX.domain.account.corporation.entity.CorporationAccount;
import com.beyond.MKX.domain.account.corporation.repository.CorporationAccountRepository;
import com.beyond.MKX.domain.account.accountlist.entity.AccountList;
import com.beyond.MKX.domain.account.accountlist.entity.AccountType;
import com.beyond.MKX.domain.account.accountlist.service.AccountListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.UUID;

/**
 * 기업 계좌 서비스
 * - 등록/승인/반려/정지 로직 관리
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CorporationAccountService {

    private final CorporationAccountRepository repository;
    private final AccountListService accountListService;
    private final TransactionEventPublisher eventPublisher;

    /** 기업이 계좌 등록 요청
     *  - 스키마(FK 제약)상 corporation_account.account_number 는 account_list.account_number 를 참조하므로
     *    등록 시점에 account_list 에도 함께 등록한다(멱등).
     */
    public CorporationAccount register(UUID corporationId, String accountNumber, AccountList ignored) {
        // 중복 계좌번호 사전 검증
        if (repository.findByAccountNumber(accountNumber).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 기업 계좌번호입니다.");
        }
        AccountList list = accountListService.registerIfAbsent(accountNumber, AccountType.CORPORATION);
        return repository.save(new CorporationAccount(corporationId, accountNumber, list));
    }

    /** 거래소 승인 */
    public void approve(UUID accountId) {
        CorporationAccount account = repository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
        // 등록 단계에서 이미 account_list 에 존재하므로 연결만 보장
        if (account.getAccountList() == null) {
            AccountList list = accountListService.registerIfAbsent(account.getAccountNumber(), AccountType.CORPORATION);
            account.attachAccountList(list);
        }
        account.approve();
        // 공통 메타 상태도 동기화
        if (account.getAccountList() != null) {
            account.getAccountList().changeStatus(AccountStatus.APPROVED);
        }
    }

    /** 거래소 승인 (계좌번호 기준) */
    public void approveByAccountNumber(String accountNumber) {
        CorporationAccount account = getByAccountNumber(accountNumber);
        approve(account.getId());
    }

    /** 거래소 반려 */
    public void reject(UUID accountId) {
        CorporationAccount account = repository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
        account.reject();
        if (account.getAccountList() != null) {
            account.getAccountList().changeStatus(AccountStatus.REJECTED);
        }
    }

    /** 거래소 반려 (계좌번호 기준) */
    public void rejectByAccountNumber(String accountNumber) {
        CorporationAccount account = getByAccountNumber(accountNumber);
        reject(account.getId());
    }

    /** 거래소 정지 */
    public void suspend(UUID accountId) {
        CorporationAccount account = repository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
        account.suspend();
        if (account.getAccountList() != null) {
            account.getAccountList().changeStatus(AccountStatus.SUSPENDED);
        }
    }

    /** 거래소 정지 (계좌번호 기준) */
    public void suspendByAccountNumber(String accountNumber) {
        CorporationAccount account = getByAccountNumber(accountNumber);
        suspend(account.getId());
    }

    /** 단건 조회 */
    public CorporationAccount getByAccountNumber(String accountNumber) {
        return repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("기업 계좌를 찾을 수 없습니다."));
    }

    public BigInteger deposit(UUID accountId, BigInteger amount) {
        CorporationAccount acc = repository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
        if (acc.getStatus() != AccountStatus.APPROVED) {
            throw new IllegalStateException("기업 계좌가 승인(APPROVED) 상태가 아닙니다.");
        }
        acc.deposit(amount);
        
        // Kafka 이벤트 발행
        eventPublisher.publishDepositEvent(acc.getId().toString(), acc.getAccountNumber(), amount.longValue(), "BANK_TRANSFER");
        
        return acc.getBalance();
    }

    public BigInteger withdraw(UUID accountId, BigInteger amount) {
        CorporationAccount acc = repository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
        if (acc.getStatus() != AccountStatus.APPROVED) {
            throw new IllegalStateException("기업 계좌가 승인(APPROVED) 상태가 아닙니다.");
        }
        acc.withdraw(amount);
        
        // Kafka 이벤트 발행
        eventPublisher.publishWithdrawalEvent(acc.getId().toString(), acc.getAccountNumber(), amount.longValue(), "BANK_TRANSFER");
        
        return acc.getBalance();
    }

    public CorporationAccount getByCorporationId(UUID corporationId) {
        return repository.findByCorporationId(corporationId)
                .orElseThrow(() -> new IllegalArgumentException("찾는 기업의 계좌가 없습니다."));
    }

    /** 기업의 모든 계좌 목록 조회 */
    public java.util.List<CorporationAccount> getAccountsByCorporationId(UUID corporationId) {
        return repository.findAllByCorporationId(corporationId);
    }
}
