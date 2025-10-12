package com.beyond.MKX.domain.assets.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "member_account",
        indexes = {
                @Index(name = "ix_member_account_member_id", columnList = "member_id"),
                @Index(name = "ix_member_account_brokerage_id", columnList = "brokerage_id"),
                @Index(name = "ix_member_account_account_number", columnList = "account_number", unique = true)
        }
)
@SQLDelete(sql = "update member_account set deleted_at = now() where id = ?")
@SQLRestriction("DELETED_AT IS NULL")
public class MemberAccount extends BaseIdAndTimeEntity {

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private UUID brokerageId;

    @Column(name = "account_number", nullable = false, columnDefinition = "VARCHAR(20)")
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Long balance = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long availableBalance = 0L;

    /** 계좌 정지 */
    public void suspend() {
        this.status = AccountStatus.SUSPENDED;
    }

    /** 계좌 재활성화 */
    public void activate() {
        this.status = AccountStatus.ACTIVE;
    }

    /** 계좌 폐쇄 (삭제) */
    public void delete() {
        this.status = AccountStatus.DELETED;
    }

    public void deposit(Long amount) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("입금 금액이 올바르지 않습니다.");
        }
        this.balance += amount;
    }

    public void withdraw(Long amount) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("출금 금액이 올바르지 않습니다.");
        }
        if (this.balance < amount) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
        this.balance -= amount;
    }
    public void decreaseAvailableBalance(Long amount) {
        this.availableBalance -= amount;
    }




    public MemberAccount(UUID memberId, UUID brokerageId, String accountNumber) {
        this.memberId = memberId;
        this.brokerageId = brokerageId;
        this.number = accountNumber;
        this.status = AccountStatus.ACTIVE;
        this.balance = 0L;
        this.availableBalance = 0L;
    }

}

