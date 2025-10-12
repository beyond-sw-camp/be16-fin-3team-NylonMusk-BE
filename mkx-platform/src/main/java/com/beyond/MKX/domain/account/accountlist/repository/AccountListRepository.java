package com.beyond.MKX.domain.account.accountlist.repository;

import com.beyond.MKX.domain.account.corporation.entity.AccountStatus;
import com.beyond.MKX.domain.account.accountlist.entity.AccountList;
import com.beyond.MKX.domain.account.accountlist.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountListRepository extends JpaRepository<AccountList, UUID> {
    Optional<AccountList> findByAccountNumber(String accountNumber);

    // 필터 조회
    List<AccountList> findByType(AccountType type);
    List<AccountList> findByStatus(AccountStatus status);
    List<AccountList> findByTypeAndStatus(AccountType type, AccountStatus status);

    // 부분 일치 검색어 결합
    List<AccountList> findByAccountNumberContaining(String accountNumber);
    List<AccountList> findByTypeAndAccountNumberContaining(AccountType type, String accountNumber);
    List<AccountList> findByStatusAndAccountNumberContaining(AccountStatus status, String accountNumber);
    List<AccountList> findByTypeAndStatusAndAccountNumberContaining(AccountType type, AccountStatus status, String accountNumber);
}
