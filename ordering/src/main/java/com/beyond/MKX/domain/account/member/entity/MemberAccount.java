package com.beyond.MKX.domain.account.member.entity;


import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.account.member.entity.MemberAccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.UUID;

/**
 *  ordering-service 전용 엔티티
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "member_account")
public class MemberAccount extends BaseIdAndTimeEntity {

    @Column(nullable = false)
    private UUID memberId; // 유저 ID

    @Column(nullable = false)
    private UUID brokerageId; // 증권사 ID

    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber; // 계좌번호

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberAccountStatus status = MemberAccountStatus.ACTIVE;

    @Column(nullable = false)
    private BigInteger balance = BigInteger.ZERO;

    public MemberAccount(UUID memberId, UUID brokerageId, String accountNumber) {
        this.memberId = memberId;
        this.brokerageId = brokerageId;
        this.accountNumber = accountNumber;
    }

    /** 계좌 정지 */
    public void suspend() {
        this.status = MemberAccountStatus.SUSPENDED;
    }

    /** 계좌 재활성화 */
    public void activate() {
        this.status = MemberAccountStatus.ACTIVE;
    }

    /** 계좌 폐쇄 (삭제) */
    public void delete() {
        this.status = MemberAccountStatus.DELETED;
    }

    public void deposit(BigInteger amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("입금 금액이 올바르지 않습니다.");
        }
        this.balance = this.balance.add(amount);
    }

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
