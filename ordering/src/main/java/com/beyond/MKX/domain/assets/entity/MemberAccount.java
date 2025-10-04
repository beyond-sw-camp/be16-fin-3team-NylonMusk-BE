package com.beyond.MKX.domain.assets.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
                @Index(name = "ix_member_account_brokerage_id", columnList = "brokerage_id")
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

    @Column(nullable = false)
    @Builder.Default
    private Long balance = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long availableBalance = 0L;

}
