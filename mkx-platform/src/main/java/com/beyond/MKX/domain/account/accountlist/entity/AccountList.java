package com.beyond.MKX.domain.account.accountlist.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.account.corporation.entity.AccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [account_list 테이블]
 * 모든 계좌의 공통 메타데이터를 관리하는 상위 테이블
 * 거래소, 증권사, 기업, 유저 계좌가 이 리스트에 등록됨.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "account_list")
public class AccountList extends BaseIdAndTimeEntity {

    /** 계좌번호 (고유) */
    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    /** 계좌 유형 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    /** 계좌 상태 (공통) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    public AccountList(String accountNumber, AccountType type) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.status = AccountStatus.APPROVED; // 기본값
    }

    public AccountList(String accountNumber, AccountType type, AccountStatus status) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.status = status;
    }

    public void changeStatus(AccountStatus status) {
        this.status = status;
    }
}
