package com.beyond.MKX.domain.account.accountlist.service;

import com.beyond.MKX.domain.account.corporation.entity.AccountStatus;
import com.beyond.MKX.domain.account.accountlist.entity.AccountList;
import com.beyond.MKX.domain.account.accountlist.entity.AccountType;
import com.beyond.MKX.domain.account.accountlist.repository.AccountListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * account_list 서비스
 *
 * 책임
 * - 공통 메타데이터(account_list)를 단일 책임으로 등록/조회한다.
 * - registerIfAbsent: 주어진 accountNumber가 없으면 저장, 있으면 기존 엔티티 반환(멱등).
 *
 * 참고
 * - account_list.account_number 는 유니크 전제이다. 동시성 환경에서는 DB 유니크 제약으로 중복을 방지한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AccountListService {

    private final AccountListRepository repository;

    /**
     * 새로운 계좌를 account_list에 등록 (멱등)
     * - 이미 존재하면 기존 엔티티 반환
     * - 존재하지 않으면 기본 상태를 계산해 저장
     */
    public AccountList registerIfAbsent(String accountNumber, AccountType type) {
        return repository.findByAccountNumber(accountNumber)
                .orElseGet(() -> repository.save(new AccountList(accountNumber, type, resolveDefaultStatus(type))));
    }

    /**
     * account_list 목록 조회
     * - type/status 조합으로 필터링, 모두 null이면 전체
     */
    @Transactional(readOnly = true)
    public List<AccountList> list(AccountType type, AccountStatus status) {
        if (type == null && status == null) {
            return repository.findAll();
        }
        if (type != null && status == null) {
            return repository.findByType(type);
        }
        if (type == null) {
            return repository.findByStatus(status);
        }
        return repository.findByTypeAndStatus(type, status);
    }

    /**
     * account_list 검색
     * - accountNumberLike 가 비어있지 않으면 부분 일치 검색을 사용
     */
    @Transactional(readOnly = true)
    public List<AccountList> search(AccountType type, AccountStatus status, String accountNumberLike) {
        boolean hasLike = accountNumberLike != null && !accountNumberLike.isBlank();
        if (!hasLike) {
            return list(type, status);
        }
        String like = accountNumberLike.trim();
        if (type == null && status == null) {
            return repository.findByAccountNumberContaining(like);
        }
        if (type != null && status == null) {
            return repository.findByTypeAndAccountNumberContaining(type, like);
        }
        if (type == null) {
            return repository.findByStatusAndAccountNumberContaining(status, like);
        }
        return repository.findByTypeAndStatusAndAccountNumberContaining(type, status, like);
    }

    /**
     * 문자열 기반 검색 (컨트롤러 단 단순화를 위해 제공)
     * - 잘못된 enum 문자열이면 IllegalArgumentException 발생
     */
    @Transactional(readOnly = true)
    public List<AccountList> search(String type, String status, String accountNumberLike) {
        AccountType typeEnum = null;
        AccountStatus statusEnum = null;
        if (type != null && !type.isBlank()) {
            try { typeEnum = AccountType.valueOf(type.toUpperCase()); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("유효하지 않은 type 입니다."); }
        }
        if (status != null && !status.isBlank()) {
            try { statusEnum = AccountStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("유효하지 않은 status 입니다."); }
        }
        return search(typeEnum, statusEnum, accountNumberLike);
    }

    /**
     * 계좌 유형별 기본 상태 결정
     * - CORPORATION: 등록 후 승인 대기 → PENDING
     * - EXCHANGE/BROKERAGE/MEMBER: 기본적으로 즉시 사용 가능 → APPROVED
     */
    private AccountStatus resolveDefaultStatus(AccountType type) {
        if (type == AccountType.CORPORATION) {
            return AccountStatus.PENDING;
        }
        // EXCHANGE, BROKERAGE, MEMBER
        return AccountStatus.APPROVED;
    }

    public void updateStatusByAccountNumber(String accountNumber, String statusStr) {
        AccountList account = repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
        AccountStatus newStatus = AccountStatus.valueOf(statusStr.toUpperCase());
        account.changeStatus(newStatus);
    }
}
