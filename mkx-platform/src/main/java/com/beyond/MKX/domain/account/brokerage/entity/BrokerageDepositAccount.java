package com.beyond.MKX.domain.account.brokerage.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.account.accountlist.entity.AccountList;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.UUID;

/**
 * 증권사 예치금 계좌
 * - 거래소가 증권사별로 관리하는 계좌
 * - 항상 유효하며 상태 개념 없음
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "brokerage_deposit_account")
public class BrokerageDepositAccount extends BaseIdAndTimeEntity {

    @Column(name = "account_number", nullable = false, unique = true, length = 20, insertable = false, updatable = false)
    private String accountNumber;

    @Column(nullable = false)
    private UUID brokerageId;

    @Column(nullable = false)
    private BigInteger deposit = BigInteger.ZERO;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_number", referencedColumnName = "account_number", nullable = false)
    private AccountList accountList;

    public BrokerageDepositAccount(UUID brokerageId, String accountNumber, AccountList accountList) {
        this.brokerageId = brokerageId;
        this.accountNumber = accountNumber;
        this.accountList = accountList;
    }

    /**
     * 입금 처리
     */
    public void deposit(BigInteger amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("입금 금액이 올바르지 않습니다.");
        }
        this.deposit = this.deposit.add(amount);
    }

    /**
     * 출금 처리 (잔액 부족 시 예외)
     */
    public void withdraw(BigInteger amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("출금 금액이 올바르지 않습니다.");
        }
        if (this.deposit.compareTo(amount) < 0) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
        this.deposit = this.deposit.subtract(amount);
    }
}
