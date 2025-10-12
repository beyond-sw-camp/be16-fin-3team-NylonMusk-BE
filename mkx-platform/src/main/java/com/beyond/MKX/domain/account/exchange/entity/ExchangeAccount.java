package com.beyond.MKX.domain.account.exchange.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.account.accountlist.entity.AccountList;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * 거래소 내부 운영 계좌 (시스템 계좌)
 * - 항상 존재해야 하며 상태 개념 없음
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "stock_exchange_account")
public class ExchangeAccount extends BaseIdAndTimeEntity {

    @Column(name = "account_number", nullable = false, unique = true, length = 20, insertable = false, updatable = false)
    private String accountNumber;

    @Column(nullable = false)
    private BigInteger balance = BigInteger.ZERO;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_number", referencedColumnName = "account_number", nullable = false)
    private AccountList accountList;

    public ExchangeAccount(String accountNumber, AccountList accountList) {
        this.accountNumber = accountNumber;
        this.accountList = accountList;
    }

    /** 입금 처리 */
    public void deposit(BigInteger amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("입금 금액이 올바르지 않습니다.");
        }
        this.balance = this.balance.add(amount);
    }

    /** 출금 처리 (잔액 부족 시 예외) */
    public void withdraw(BigInteger amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("출금 금액이 올바르지 않습니다.");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
        this.balance = this.balance.subtract(amount);
    }
}
