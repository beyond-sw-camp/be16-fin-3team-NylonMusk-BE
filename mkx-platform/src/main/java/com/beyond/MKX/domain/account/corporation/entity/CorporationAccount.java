package com.beyond.MKX.domain.account.corporation.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.account.accountlist.entity.AccountList;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.UUID;

/**
 * 기업 등록 계좌
 * - 기업이 등록 요청 → 거래소가 승인/반려 처리
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "corporation_account")
public class CorporationAccount extends BaseIdAndTimeEntity {

    // 기업당 계좌 1개 보유 보장 (DB 유니크)
    @Column(nullable = false, unique = true)
    private UUID corporationId;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.PENDING;

    @Column(nullable = false)
    private BigInteger balance = BigInteger.ZERO;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_number", referencedColumnName = "account_number", nullable = true, insertable = false, updatable = false)
    private AccountList accountList;

    public CorporationAccount(UUID corporationId, String accountNumber, AccountList accountList) {
        this.corporationId = corporationId;
        this.accountNumber = accountNumber;
        this.accountList = accountList;
    }

    /** 거래소 승인 */
    public void approve() { this.status = AccountStatus.APPROVED; }

    /** 거래소 반려 */
    public void reject() { this.status = AccountStatus.REJECTED; }

    /** 거래소 일시 정지 */
    public void suspend() { this.status = AccountStatus.SUSPENDED; }

    /** 승인 시 account_list 연결 */
    public void attachAccountList(AccountList list) { this.accountList = list; }

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
